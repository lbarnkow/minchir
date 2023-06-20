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
    models::{CompletedRequest, LogoutRequest, RejectRequest},
};
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use tera::Tera;

const TEMPLATE: &str = "logout.html";

#[derive(Debug, Serialize)]
struct Context {
    lang: String,
    csrf_token: String,
    logout_challenge: String,
    subject: String,
}

pub(crate) async fn get(
    Extension(settings): Extension<Settings>,
    Extension(hydra_api_cfg): Extension<Configuration>,
    AcceptLanguage(lang): AcceptLanguage,
    Extension(tera): Extension<Tera>,
    Query(params): Query<HashMap<String, String>>,
    csrf_data: CsrfData,
) -> Result<Response, StatusCode> {
    let logout_challenge = params.get("logout_challenge").ok_or_else(|| {
        tracing::error!("Mandatory query param 'logout_challenge' missing!");
        StatusCode::BAD_REQUEST
    })?;

    let logout_request = get_hydra_logout_challenge(logout_challenge, &hydra_api_cfg).await?;

    tracing::debug!("Rendering logout page.");

    let subject = get_subject_information(logout_request.subject)?;

    let ctx = Context {
        lang: lang
            .or(Some(
                settings.config.server().default_language().to_string(),
            ))
            .unwrap(),
        csrf_token: csrf_data.token.clone(),
        logout_challenge: logout_challenge.clone(),
        subject,
    };

    Ok((csrf_data, handlers::render_template(&tera, TEMPLATE, &ctx)).into_response())
}

#[derive(Debug, Deserialize)]
pub struct ConsentForm {
    logout_challenge: Option<String>,
    logout: Option<String>,
    cancel: Option<String>,
}

pub(crate) async fn post(
    Extension(hydra_api_cfg): Extension<Configuration>,
    _: CsrfData,
    Form(form): Form<ConsentForm>,
) -> Result<Response, StatusCode> {
    if !(form.logout.is_some() ^ form.cancel.is_some()) {
        tracing::error!("form was not submitted properly!");
        return Err(StatusCode::BAD_REQUEST);
    }

    let logout_challenge = handlers::assert_mandatory_form_field_is_set(form.logout_challenge)?;

    if form.cancel.is_some() {
        tracing::debug!("user submitted logout form via cancel button.");
        reject_logout_request(&logout_challenge, &hydra_api_cfg).await?;
        // TODO
        return Ok(Redirect::to("https://www.rust-lang.org/").into_response());
    }

    tracing::debug!("user submitted logout form via logout button.");

    let logout_challenge_response =
        get_hydra_logout_challenge(&logout_challenge, &hydra_api_cfg).await?;
    tracing::info!(
        "INFO: User '{}' successfully logged out.",
        logout_challenge_response
            .subject
            .or(Some("?".to_string()))
            .unwrap()
    );

    let response = accept_logout_request(&logout_challenge, &hydra_api_cfg).await?;
    return Ok(Redirect::to(&response.redirect_to).into_response());
}

async fn get_hydra_logout_challenge(
    logout_challenge: &str,
    hydra_api_cfg: &Configuration,
) -> Result<LogoutRequest, StatusCode> {
    admin_api::get_logout_request(hydra_api_cfg, logout_challenge)
        .await
        .map_err(handlers::map_hydra_error)
}

async fn accept_logout_request(
    logout_challenge: &str,
    hydra_api_cfg: &Configuration,
) -> Result<CompletedRequest, StatusCode> {
    admin_api::accept_logout_request(hydra_api_cfg, logout_challenge)
        .await
        .map_err(handlers::map_hydra_error)
}

async fn reject_logout_request(
    logout_challenge: &str,
    hydra_api_cfg: &Configuration,
) -> Result<(), StatusCode> {
    admin_api::reject_logout_request(
        hydra_api_cfg,
        logout_challenge,
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

fn get_subject_information(subject: Option<String>) -> Result<String, StatusCode> {
    subject.ok_or_else(|| {
        tracing::error!("hydra consent request is missing subject information!");
        StatusCode::INTERNAL_SERVER_ERROR
    })
}

// #[cfg(test)]
// mod tests {


//     fn unit_mytest() {
//         println!("Ok :D");
//         panic!("Show output.");
//     }
// }