use crate::{
    handlers::{consent, csrf::CsrfLayer, login},
    settings::{config::Config, scopes::Scopes, translations::Translations},
};
use axum::{
    error_handling::HandleErrorLayer,
    extract::DefaultBodyLimit,
    http::StatusCode,
    middleware::from_fn,
    response::IntoResponse,
    routing::{get, get_service},
    BoxError, Extension, Router,
};
use cli::Args;
use error::MinchirError;
use handlers::logout;
use middleware::ldap::LdapLayer;
use ory_hydra_client::apis::configuration::Configuration;
use serde_json::Value;
use std::{collections::HashMap, fs::File, io::Read, sync::Arc, time::Duration};
use tera::Tera;
use tower::{timeout::TimeoutLayer, ServiceBuilder};
use tower_cookies::CookieManagerLayer;
use tower_http::{
    limit::RequestBodyLimitLayer,
    services::ServeDir,
    trace::{
        DefaultMakeSpan, DefaultOnEos, DefaultOnFailure, DefaultOnRequest, DefaultOnResponse,
        TraceLayer,
    },
    ServiceBuilderExt,
};
use tracing::Level;
use unic_langid::LanguageIdentifier;

pub mod cli;
pub mod error;
pub(crate) mod handlers;
pub(crate) mod middleware;
pub(crate) mod settings;

pub type Res<T> = Result<T, MinchirError>;

pub type Settings = Arc<settings::Settings>;

const MAX_REQUEST_BODY_SIZE: usize = 1024 * 4; // 4 KiB

pub fn lookup_translation(
    t: &Translations,
    value: &Value,
    args: &HashMap<String, Value>,
    default_lang: &str,
) -> Result<Value, tera::Error> {
    let lang = args
        .get("lang")
        .map(|v| v.as_str().or(Some(default_lang)).unwrap())
        .or(Some(default_lang))
        .unwrap();

    if !value.is_string() {
        return Err(tera::Error::msg(format!(
            "Translation key '{}' is not a string!",
            value.to_string()
        )));
    }

    let lang_id = LanguageIdentifier::from_bytes(lang.as_bytes()).unwrap();

    Ok(Value::String(
        t.lookup(value.as_str().unwrap(), &lang_id, default_lang)
            .to_string(),
    ))
}

pub fn minchir(args: &Args) -> Res<Minchir> {
    let settings = Arc::new(settings::Settings {
        config: Config::load(args.configs())?,
        scopes: Scopes::load(args.scopes())?,
        translations: Translations::load(args.translations())?,
    });

    let mut tera = Tera::new("assets/templates/*.html")?;
    tera.autoescape_on(vec![]);
    let tera_trans = settings.translations.clone();
    tera.register_filter("_", move |value: &Value, m: &HashMap<String, Value>| {
        return lookup_translation(&tera_trans, value, m, "en");
    });

    let hydra_api_cfg = get_hydra_admin_configuration(settings.config.clone())?;

    let app = Router::new() //
        .route("/login", get(login::get).post(login::post))
        .route("/consent", get(consent::get).post(consent::post))
        .route("/logout", get(logout::get).post(logout::post))
        .route("/hello-world", get(|| async { "Hello, World!" }))
        .fallback(
            get_service(ServeDir::new(
                settings.config.server().assets_path("static"),
            ))
            .handle_error(static_error),
        )
        .layer(
            TraceLayer::new_for_http()
                .make_span_with(DefaultMakeSpan::new().include_headers(false))
                .on_request(DefaultOnRequest::new().level(Level::TRACE))
                .on_response(DefaultOnResponse::new().level(Level::TRACE))
                .on_failure(DefaultOnFailure::new().level(Level::TRACE))
                .on_eos(DefaultOnEos::new().level(Level::TRACE)),
        )
        .layer(
            ServiceBuilder::new()
                .layer(Extension(tera))
                .layer(Extension(settings.clone()))
                .layer(Extension(hydra_api_cfg))
                .layer(from_fn(handlers::render_error))
                .layer(DefaultBodyLimit::disable())
                .layer(RequestBodyLimitLayer::new(MAX_REQUEST_BODY_SIZE))
                .layer(HandleErrorLayer::new(|_: BoxError| async {
                    StatusCode::REQUEST_TIMEOUT
                }))
                .layer(TimeoutLayer::new(Duration::from_secs(30)))
                .layer(CookieManagerLayer::new())
                .layer(CsrfLayer::new(settings.config.clone())?)
                .layer(LdapLayer::new(settings.config.clone())?)
                .map_request_body(axum::body::boxed),
        );

    Ok(Minchir {
        settings,
        router: app,
    })
}

async fn static_error(_err: std::io::Error) -> impl IntoResponse {
    StatusCode::NOT_FOUND
}

pub struct Minchir {
    pub settings: Settings,
    pub router: Router,
}

fn get_hydra_admin_configuration(config: Config) -> Res<Configuration> {
    let config = config.hydra();
    let mut builder = reqwest::ClientBuilder::new();

    if *config.skip_tls_verification() {
        tracing::warn!(
            "WARN: skipping certificate verification when accessing hydra api is very insecure!"
        );
        builder = builder.danger_accept_invalid_certs(*config.skip_tls_verification())
    }
    if config.ca_bundle().is_some() {
        let mut buf = Vec::new();
        File::open(config.ca_bundle().as_ref().unwrap().clone())
            .map_err(|error| MinchirError::HydraApiClientCaBundleError { source: error })?
            .read_to_end(&mut buf)
            .map_err(|error| MinchirError::HydraApiClientCaBundleError { source: error })?;
        let cert = reqwest::Certificate::from_pem(&buf)
            .map_err(|error| MinchirError::HydraApiClientBuildError { source: error })?;
        builder = builder.add_root_certificate(cert);
    }

    let client = builder.build().map_err(map_hydra_api_init_error)?;

    Ok(Configuration {
        base_path: config.admin_url().clone(),
        user_agent: Some("Minchir".to_string()),
        client,
        basic_auth: None,
        oauth_access_token: None,
        bearer_access_token: None,
        api_key: None,
    })
}

fn map_hydra_api_init_error(error: reqwest::Error) -> MinchirError {
    MinchirError::HydraApiClientBuildError { source: error }
}
