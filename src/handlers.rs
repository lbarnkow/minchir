use axum::response::{Html, IntoResponse, Response};
use axum::{http::Request, middleware::Next};
use http::StatusCode;
use serde::Serialize;
use tera::{Context, Tera};

pub mod csrf;

pub(crate) mod consent;
pub(crate) mod login;
pub(crate) mod logout;

#[derive(Serialize)]
struct EmptyContext {
    lang: String,
}

const TEMPLATED_ERRORS: [StatusCode; 3] = [
    StatusCode::BAD_REQUEST,
    StatusCode::NOT_FOUND,
    StatusCode::INTERNAL_SERVER_ERROR,
];

pub(crate) async fn render_error<B>(
    req: Request<B>,
    next: Next<B>,
) -> Result<Response, StatusCode> {
    let tera = req
        .extensions()
        .get::<Tera>()
        .ok_or_else(|| {
            tracing::error!("Extension 'Tera' is missing!");
            StatusCode::INTERNAL_SERVER_ERROR
        })?
        .clone();

    let mut response = next.run(req).await;

    if TEMPLATED_ERRORS.contains(&response.status()) {
        let template = format!("{}.html", response.status().as_u16());
        let html = render_template(
            &tera,
            &template,
            &EmptyContext {
                lang: "en".to_string(),
            },
        )?;
        response = (response.status(), html.into_response()).into_response();
    }

    Ok(response)
}

pub(crate) fn render_template<T>(
    tera: &Tera,
    template: &str,
    ctx: &T,
) -> Result<Html<String>, StatusCode>
where
    T: Serialize,
{
    let ctx = Context::from_serialize(ctx).map_err(|error| {
        tracing::error!("failed to render template {template}, reason: {error:?}!");
        StatusCode::INTERNAL_SERVER_ERROR
    })?;
    let rendered = tera.render(template, &ctx).map_err(|error| {
        tracing::error!("failed to render template {template}, reason: {error:?}!");
        StatusCode::INTERNAL_SERVER_ERROR
    })?;

    Ok(Html(rendered))
}

fn assert_mandatory_form_field_is_set<T>(field: Option<T>) -> Result<T, StatusCode> {
    field.ok_or_else(|| {
        tracing::error!("Mandatory consent form field missing!");
        return StatusCode::BAD_REQUEST;
    })
}

fn map_hydra_error<T>(error: ory_hydra_client::apis::Error<T>) -> StatusCode
where
    T: std::fmt::Debug,
{
    match error {
        ory_hydra_client::apis::Error::ResponseError(re) => {
            tracing::error!(
                "hydra responded with status: {} - content: {}.",
                re.status.as_u16(),
                re.content.replace('\n', "")
            );
            StatusCode::BAD_REQUEST
        }
        _ => {
            tracing::error!("failed to contact hydra: {error:?}");
            StatusCode::INTERNAL_SERVER_ERROR
        }
    }
}
