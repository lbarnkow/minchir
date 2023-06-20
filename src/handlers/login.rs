use super::csrf::CsrfData;
use crate::{
    handlers::{self},
    middleware::{accept_language::AcceptLanguage, ldap::LdapLayer},
    settings::config::Config,
    Settings,
};
use axum::{
    extract::Query,
    http::StatusCode,
    response::{IntoResponse, Redirect, Response},
    Extension, Form,
};
use ldap3::{Scope, SearchEntry};
use ory_hydra_client::{
    apis::{admin_api, configuration::Configuration},
    models::{AcceptLoginRequest, CompletedRequest, LoginRequest, RejectRequest},
};
use serde::{Deserialize, Serialize};
use tracing::instrument;
use std::collections::HashMap;
use tera::Tera;

const TEMPLATE: &str = "login.html";

#[derive(Debug, Serialize)]
struct Context {
    lang: String,
    csrf_token: String,
    login_challenge: String,
    error_bad_credentials: bool,
    username: String,
    remember_me: bool,
}

#[instrument(skip_all)]
pub(crate) async fn get(
    Extension(settings): Extension<Settings>,
    Extension(hydra_api_cfg): Extension<Configuration>,
    AcceptLanguage(lang): AcceptLanguage,
    Extension(tera): Extension<Tera>,
    Query(params): Query<HashMap<String, String>>,
    csrf_data: CsrfData,
) -> Result<Response, StatusCode> {
    let login_challenge = params.get("login_challenge").ok_or_else(|| {
        tracing::error!("Mandatory query param 'login_challenge' missing!");
        StatusCode::BAD_REQUEST
    })?;

    let login_request = get_hydra_login_challenge(login_challenge, &hydra_api_cfg).await?;

    if login_request.skip {
        tracing::debug!("Skipping login form as requested by ory hydra via login challenge!");
        let response = accept_login_request(login_request, &settings, &hydra_api_cfg, true).await?;
        return Ok(Redirect::to(&response.redirect_to).into_response());
    }

    tracing::debug!("Rendering login page.");
    let ctx = Context {
        lang: lang
            .or(Some(
                settings.config.server().default_language().to_string(),
            ))
            .unwrap(),
        csrf_token: csrf_data.token.clone(),
        login_challenge: login_request.challenge,
        error_bad_credentials: false,
        username: String::new(),
        remember_me: false,
    };
    Ok((csrf_data, handlers::render_template(&tera, TEMPLATE, &ctx)).into_response())
}

#[derive(Debug, Deserialize)]
pub struct LoginForm {
    login_challenge: Option<String>,
    username: Option<String>,
    password: Option<String>,
    totp: Option<String>,
    remember_me: Option<String>,
    login: Option<String>,
    cancel: Option<String>,
}

pub(crate) async fn post(
    Extension(settings): Extension<Settings>,
    Extension(hydra_api_cfg): Extension<Configuration>,
    AcceptLanguage(lang): AcceptLanguage,
    Extension(tera): Extension<Tera>,
    csrf_data: CsrfData,
    Form(form): Form<LoginForm>,
    ldap: LdapLayer,
) -> Result<Response, StatusCode> {
    if !(form.login.is_some() ^ form.cancel.is_some()) {
        tracing::error!("form was not submitted properly!");
        return Err(StatusCode::BAD_REQUEST);
    }

    let login_challenge = handlers::assert_mandatory_form_field_is_set(form.login_challenge)?;

    if form.cancel.is_some() {
        tracing::debug!("user submitted login form via cancel button.");
        let response = reject_login_request(&login_challenge, &hydra_api_cfg).await?;
        return Ok(Redirect::to(&response.redirect_to).into_response());
    }

    tracing::debug!("user submitted login form via login button.");

    let mut login_request = get_hydra_login_challenge(&login_challenge, &hydra_api_cfg).await?;

    let username = handlers::assert_mandatory_form_field_is_set(form.username)?;
    let password = handlers::assert_mandatory_form_field_is_set(form.password)?;
    let totp = handlers::assert_mandatory_form_field_is_set(form.totp)?;
    let remember_me = form
        .remember_me
        .map_or_else(|| false, |s| s.parse::<bool>().unwrap_or(false));

    if check_credentials(&settings.config, &ldap, &username, &password, &totp).await? {
        tracing::info!("User '{username}' successfully logged in.");
        login_request.subject = username;
        let response =
            accept_login_request(login_request, &settings, &hydra_api_cfg, remember_me).await?;
        return Ok(Redirect::to(&response.redirect_to).into_response());
    }

    tracing::warn!("Failed to authenticate user '{username}'!");
    tracing::debug!("Rendering login page.");
    let ctx = Context {
        lang: lang
            .or(Some(
                settings.config.server().default_language().to_string(),
            ))
            .unwrap(),
        csrf_token: csrf_data.token.clone(),
        login_challenge: login_request.challenge,
        error_bad_credentials: true,
        username: username,
        remember_me: remember_me,
    };
    Ok((
        StatusCode::UNAUTHORIZED,
        csrf_data,
        handlers::render_template(&tera, TEMPLATE, &ctx),
    )
        .into_response())
}

async fn check_credentials(
    cfg: &Config,
    ldap: &LdapLayer,
    username: &str,
    password: &str,
    totp: &str,
) -> Result<bool, StatusCode> {
    if username.trim().is_empty() {
        tracing::warn!("Submitted username was empty!");
        return Ok(false);
    }

    let cfg = cfg.ldap();

    let mut ldap = ldap.get_connection().await.map_err(|error| {
        tracing::error!("{error}");
        StatusCode::INTERNAL_SERVER_ERROR
    })?;

    let bind = ldap
        .simple_bind(&cfg.bind_dn(), &cfg.bind_password())
        .await
        .map_err(|error| {
            tracing::error!("{error}");
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

    bind.success().map_err(|error| {
        tracing::error!("{error}");
        StatusCode::INTERNAL_SERVER_ERROR
    })?;

    let filter = format!(
        "(&(objectClass={})({}={}))",
        cfg.user_search_object_class(),
        cfg.user_attribute_uid(),
        username
    );
    let attrs = ["dn", cfg.user_attribute_uid()];

    let search = ldap
        .search(&cfg.user_search_base_dn(), Scope::Subtree, &filter, attrs)
        .await
        .map_err(|error| {
            tracing::error!("{error}");
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

    let (mut search, _) = search.success().map_err(|error| {
        tracing::error!("{error}");
        StatusCode::INTERNAL_SERVER_ERROR
    })?;

    if search.len() == 0 {
        tracing::info!("User '{username}' not found in LDAP search!");
        return Ok(false);
    } else if search.len() > 1 {
        tracing::warn!(
            "WARN: Found too many objects ({}) matching the filter '{}'!",
            search.len(),
            filter
        );
        return Ok(false);
    }

    let entry = SearchEntry::construct(search.pop().unwrap());
    let bind_pw = format!("{}{}", password, totp);

    let bind = ldap
        .simple_bind(&entry.dn, &bind_pw)
        .await
        .map_err(|error| {
            tracing::error!("{error}");
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

    Ok(bind.success().is_ok())
}

async fn get_hydra_login_challenge(
    login_challenge: &str,
    hydra_api_cfg: &Configuration,
) -> Result<LoginRequest, StatusCode> {
    admin_api::get_login_request(&hydra_api_cfg, login_challenge)
        .await
        .map_err(handlers::map_hydra_error)
}

async fn accept_login_request(
    login_request: LoginRequest,
    settings: &Settings,
    hydra_api_cfg: &Configuration,
    remember_me: bool,
) -> Result<CompletedRequest, StatusCode> {
    admin_api::accept_login_request(
        &hydra_api_cfg,
        &login_request.challenge,
        Some(AcceptLoginRequest {
            subject: login_request.subject,
            remember: Some(remember_me),
            remember_for: Some(*settings.config.hydra().remember_for_s() as i64),
            acr: None,
            amr: None,
            context: None,
            force_subject_identifier: None,
        }),
    )
    .await
    .map_err(handlers::map_hydra_error)
}

async fn reject_login_request(
    login_challenge: &str,
    hydra_api_cfg: &Configuration,
) -> Result<CompletedRequest, StatusCode> {
    admin_api::reject_login_request(
        &hydra_api_cfg,
        login_challenge,
        Some(RejectRequest {
            error: Some("access_denied".to_string()),
            error_debug: Some("The resource owner denied the request".to_string()),
            error_description: None,
            error_hint: None,
            status_code: None,
        }),
    )
    .await
    .map_err(handlers::map_hydra_error)
}
