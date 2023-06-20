use super::csrf::CsrfData;
use crate::{
    handlers::{self},
    middleware::accept_language::AcceptLanguage,
    Settings,
};
use axum::{
    extract::Query,
    http::StatusCode,
    response::{IntoResponse, Redirect, Response},
    Extension, Form,
};
use ory_hydra_client::{
    apis::{admin_api, configuration::Configuration},
    models::{AcceptConsentRequest, CompletedRequest, ConsentRequest, OAuth2Client, RejectRequest},
};
use serde::{Deserialize, Serialize};
use std::{collections::HashMap, vec};
use tera::Tera;

const TEMPLATE: &str = "consent.html";

#[derive(Debug, Serialize)]
struct Context {
    lang: String,
    csrf_token: String,
    consent_challenge: String,
    scopes: HashMap<String, Vec<String>>,
    client: String,
    subject: String,
    remember_me: bool,
}

pub(crate) async fn get(
    Extension(settings): Extension<Settings>,
    Extension(hydra_api_cfg): Extension<Configuration>,
    AcceptLanguage(lang): AcceptLanguage,
    Extension(tera): Extension<Tera>,
    Query(params): Query<HashMap<String, String>>,
    csrf_data: CsrfData,
) -> Result<Response, StatusCode> {
    let consent_challenge = params.get("consent_challenge").ok_or_else(|| {
        tracing::error!("Mandatory query param 'consent_challenge' missing!");
        StatusCode::BAD_REQUEST
    })?;

    let consent_request = get_hydra_consent_challenge(consent_challenge, &hydra_api_cfg).await?;

    if consent_request.skip.or(Some(false)).unwrap() {
        tracing::debug!("Skipping consent form as requested by ory hydra via consent challenge!");
        let response = accept_consent_request(
            &consent_request.challenge,
            &settings,
            &hydra_api_cfg,
            true,
            consent_request.requested_access_token_audience,
            consent_request.requested_scope,
        )
        .await?;
        return Ok(Redirect::to(&response.redirect_to).into_response());
    }

    tracing::debug!("Rendering consent page.");

    let client = get_client_information(consent_request.client)?;
    let subject = get_subject_information(consent_request.subject)?;
    let scopes = prepare_scopes_and_claims(consent_request.requested_scope);

    let ctx = Context {
        lang: lang
            .or(Some(
                settings.config.server().default_language().to_string(),
            ))
            .unwrap(),
        csrf_token: csrf_data.token.clone(),
        consent_challenge: consent_request.challenge,
        scopes,
        client,
        subject,
        remember_me: false,
    };

    Ok((csrf_data, handlers::render_template(&tera, TEMPLATE, &ctx)).into_response())
}

#[derive(Debug, Deserialize)]
pub struct ConsentForm {
    consent_challenge: Option<String>,
    remember_consent: Option<String>,
    consent: Option<String>,
    cancel: Option<String>,
}

pub(crate) async fn post(
    Extension(settings): Extension<Settings>,
    Extension(hydra_api_cfg): Extension<Configuration>,
    _: CsrfData,
    Form(form): Form<ConsentForm>,
) -> Result<Response, StatusCode> {
    if !(form.consent.is_some() ^ form.cancel.is_some()) {
        tracing::error!("form was not submitted properly!");
        return Err(StatusCode::BAD_REQUEST);
    }

    let consent_challenge = handlers::assert_mandatory_form_field_is_set(form.consent_challenge)?;

    if form.cancel.is_some() {
        tracing::debug!("user submitted consent form via cancel button.");
        let response = reject_consent_request(&consent_challenge, &hydra_api_cfg).await?;
        return Ok(Redirect::to(&response.redirect_to).into_response());
    }

    tracing::debug!("user submitted consent form via login button.");

    let remember_me = form
        .remember_consent
        .map_or_else(|| false, |s| s.parse::<bool>().unwrap_or(false));
    let consent_request = get_hydra_consent_challenge(&consent_challenge, &hydra_api_cfg).await?;
    let client = get_client_information(consent_request.client)?;
    let subject = get_subject_information(consent_request.subject)?;

    tracing::info!(
        "subject '{}' consented for client '{}'.",
        subject, client
    );
    let response = accept_consent_request(
        &consent_request.challenge,
        &settings,
        &hydra_api_cfg,
        remember_me,
        consent_request.requested_access_token_audience,
        consent_request.requested_scope,
    )
    .await?;
    return Ok(Redirect::to(&response.redirect_to).into_response());
}

async fn get_hydra_consent_challenge(
    consent_challenge: &str,
    hydra_api_cfg: &Configuration,
) -> Result<ConsentRequest, StatusCode> {
    admin_api::get_consent_request(hydra_api_cfg, consent_challenge)
        .await
        .map_err(handlers::map_hydra_error)
}

async fn accept_consent_request(
    consent_challenge: &str,
    settings: &Settings,
    hydra_api_cfg: &Configuration,
    remember_me: bool,
    grant_access_token_audience: Option<Vec<String>>,
    grant_scope: Option<Vec<String>>,
) -> Result<CompletedRequest, StatusCode> {
    admin_api::accept_consent_request(
        hydra_api_cfg,
        consent_challenge,
        Some(AcceptConsentRequest {
            grant_access_token_audience,
            grant_scope,
            handled_at: None,
            remember: Some(remember_me),
            remember_for: Some(*settings.config.hydra().remember_for_s() as i64),
            session: None,
        }),
    )
    .await
    .map_err(handlers::map_hydra_error)
}

async fn reject_consent_request(
    consent_challenge: &str,
    hydra_api_cfg: &Configuration,
) -> Result<CompletedRequest, StatusCode> {
    admin_api::reject_consent_request(
        hydra_api_cfg,
        consent_challenge,
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

fn get_client_information(client: Option<Box<OAuth2Client>>) -> Result<String, StatusCode> {
    let client = match client {
        Some(c) => c.client_name.or(c.client_id.or(None)),
        None => None,
    };
    client.ok_or_else(|| {
        tracing::error!("hydra consent request is missing client information!");
        StatusCode::INTERNAL_SERVER_ERROR
    })
}

fn get_subject_information(subject: Option<String>) -> Result<String, StatusCode> {
    subject.ok_or_else(|| {
        tracing::debug!("hydra consent request is missing subject information!");
        StatusCode::INTERNAL_SERVER_ERROR
    })
}

fn prepare_scopes_and_claims(scopes: Option<Vec<String>>) -> HashMap<String, Vec<String>> {
    let scopes = scopes.or(Some(Vec::new())).unwrap();

    let mut result = HashMap::new();

    for scope in scopes {
        let claims = vec![
            "Claim 1".to_string(),
            "Claim 2".to_string(),
            "Claim 3".to_string(),
        ];

        result.insert(scope, claims);
    }
    // TODO

    result
}
