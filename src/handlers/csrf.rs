use std::{any::type_name, borrow::Cow, sync::Arc};

use axum::{
    async_trait, body,
    body::BoxBody,
    body::Full,
    extract::{FromRequest, RequestParts},
    http::Method,
    http::Request,
    http::{header, StatusCode},
    response::{IntoResponse, IntoResponseParts, Response, ResponseParts},
    Extension,
};
use cookie::SameSite;
use derive_builder::Builder;
use http::HeaderValue;
use tower_cookies::{Cookie, Cookies};

use crate::{error::MinchirError, Res};

use self::supplier::CsrfSupplier;

mod supplier;

#[derive(Builder, Clone)]
#[builder(setter(into))]
pub struct CsrfConfig {
    #[builder(default = r#""csrf_token".to_string()"#)]
    form_field_name: String,
    #[builder(default = r#""csrf_cookie".to_string()"#)]
    cookie_name: String,
    #[builder(default = "true")]
    cookie_http_only: bool,
    #[builder(default = "true")]
    cookie_secure: bool,
    #[builder(default = "None")]
    cookie_domain: Option<String>,
    #[builder(default = r#""/".to_string()"#)]
    cookie_path: String,
    #[builder(default = "cookie::SameSite::Strict")]
    cookie_samesite: SameSite,
    #[builder(default = "300")]
    totp_ttl_s: u64,
    totp_key: Option<String>,
    hmac_key: Option<String>,
}

impl CsrfConfig {
    pub fn into_cookie<'a, T>(&'a self, value: T) -> Cookie<'a>
    where
        T: Into<Cow<'a, str>>,
    {
        let mut c = Cookie::build(&self.cookie_name, value)
        .http_only(self.cookie_http_only)
        .secure(self.cookie_secure)
        .path(&self.cookie_path)
        .same_site(self.cookie_samesite)
        //.max_age()
        ;

        if self.cookie_domain.is_some() {
            c = c.domain(self.cookie_domain.clone().unwrap());
        }

        c.finish()
    }
}

#[derive(Clone)]
pub struct CsrfLayer {
    config: Arc<CsrfConfig>,
    supplier: Arc<CsrfSupplier>,
}

impl CsrfLayer {
    pub fn new<T>(cfg: T) -> Res<Extension<Self>>
    where
        T: Into<CsrfConfig>,
    {
        let cfg: CsrfConfig = cfg.into();

        let supplier = CsrfSupplier::build(&cfg)?;

        Ok(Extension(Self {
            config: Arc::new(cfg),
            supplier: Arc::new(supplier),
        }))
    }
}

pub struct CsrfData {
    pub token: String,
    pub cookie: String,
    csrf_config: Arc<CsrfConfig>,
}

impl std::fmt::Debug for CsrfData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("CsrfData")
            .field("token", &self.token)
            .field("cookie", &self.cookie)
            .finish()
    }
}

const READ_ONLY_METHODS: [Method; 4] = [Method::GET, Method::HEAD, Method::OPTIONS, Method::TRACE];

#[async_trait]
impl FromRequest<BoxBody> for CsrfData {
    type Rejection = Response;

    async fn from_request(req: &mut RequestParts<BoxBody>) -> Result<Self, Self::Rejection> {
        if READ_ONLY_METHODS.contains(req.method()) {
            return generate_csrf_token_and_cookie(req);
        } else {
            return verify_csrf_token_and_cookie(req).await;
        }
    }
}

impl IntoResponseParts for CsrfData {
    type Error = StatusCode;

    fn into_response_parts(self, mut res: ResponseParts) -> Result<ResponseParts, Self::Error> {
        let csrf_config = self.csrf_config;
        let cookie = csrf_config.into_cookie(self.cookie);

        let k = header::SET_COOKIE;
        let v = HeaderValue::from_str(&cookie.to_string()).map_err(|error| {
            tracing::error!("{error:?}");
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

        res.headers_mut().insert(k, v);

        Ok(res)
    }
}

fn get_extension_from_request_parts<T>(req: &RequestParts<BoxBody>) -> Result<&T, Response>
where
    T: 'static + Send + Sync,
{
    req.extensions().get::<T>().ok_or_else(|| {
        tracing::error!("Extension '{}' is missing!", type_name::<T>());
        StatusCode::INTERNAL_SERVER_ERROR.into_response()
    })
}

fn generate_csrf_token_and_cookie(req: &RequestParts<BoxBody>) -> Result<CsrfData, Response> {
    let csrf_layer = get_extension_from_request_parts::<CsrfLayer>(req)?;

    let (totp, hmac) = csrf_layer.supplier.generate().or_else(|error| {
        tracing::error!("{error:?}");
        Err(StatusCode::INTERNAL_SERVER_ERROR.into_response())
    })?;
    return Ok(CsrfData {
        token: totp,
        cookie: hmac,
        csrf_config: csrf_layer.config.clone(),
    });
}

async fn verify_csrf_token_and_cookie(
    req: &mut RequestParts<BoxBody>,
) -> Result<CsrfData, Response> {
    if !assert_form_content_type(req) {
        return Err(StatusCode::BAD_REQUEST.into_response());
    }

    let request = Request::from_request(req).await.map_err(|error| {
        tracing::error!("{error:?}");
        return StatusCode::INTERNAL_SERVER_ERROR.into_response();
    })?;

    let (new_req, form_data) = extract_form_data_from_request(request).await?;
    *req = RequestParts::new(new_req); // restore request body to unconsumed state

    let csrf_layer = get_extension_from_request_parts::<CsrfLayer>(req)?;

    let csrf_cookie = extract_csrf_cookie_from_request(req, &csrf_layer.config.cookie_name)?;
    let csrf_token =
        extract_csrf_token_from_form_data(&form_data, &csrf_layer.config.form_field_name)?;

    csrf_layer
        .supplier
        .verify(&csrf_token, &csrf_cookie)
        .map_err(|error| {
            tracing::error!("{error:?}");
            match error {
                MinchirError::TotpVerificationError => StatusCode::BAD_REQUEST.into_response(),
                MinchirError::HmacBase32DecodingError { encoded: _ } => {
                    StatusCode::BAD_REQUEST.into_response()
                }
                MinchirError::HmacVerificationError { source: _ } => {
                    StatusCode::BAD_REQUEST.into_response()
                }
                _ => StatusCode::INTERNAL_SERVER_ERROR.into_response(),
            }
        })?;

    return Ok(CsrfData {
        token: csrf_token,
        cookie: csrf_cookie,
        csrf_config: csrf_layer.config.clone(),
    });
}

fn assert_form_content_type(req: &RequestParts<BoxBody>) -> bool {
    let header = req.headers().get(header::CONTENT_TYPE);
    if header.is_none() {
        tracing::error!("Header content-type is missing!");
        return false;
    }
    let content_type = header.unwrap().to_str();

    if content_type.is_err() {
        tracing::error!("Header content-type is not valid utf-8!");
        return false;
    }

    let success = content_type
        .unwrap()
        .to_lowercase()
        .starts_with(mime::APPLICATION_WWW_FORM_URLENCODED.as_ref());

    if !success {
        tracing::error!("Header content-type is not set to 'application/x-www-form-urlencoded'!");
    }

    success
}

async fn extract_form_data_from_request(
    request: Request<BoxBody>,
) -> Result<(Request<BoxBody>, Vec<(String, String)>), Response> {
    let (parts, body) = request.into_parts();

    let bytes = hyper::body::to_bytes(body).await.map_err(|err| {
        tracing::error!("{err:?}");
        StatusCode::INTERNAL_SERVER_ERROR.into_response()
    })?;

    let form_data =
        serde_urlencoded::from_bytes::<Vec<(String, String)>>(&bytes.clone()).map_err(|_| {
            tracing::error!("Failed to deserialize urlencoded form data!");
            StatusCode::BAD_REQUEST.into_response()
        })?;

    return Ok((
        Request::from_parts(parts, body::boxed(Full::from(bytes))),
        form_data,
    ));
}

fn extract_csrf_cookie_from_request(
    req: &RequestParts<BoxBody>,
    cookie_name: &str,
) -> Result<String, Response> {
    let cookies = get_extension_from_request_parts::<Cookies>(req)?;

    let csrf_cookie = cookies
        .get(cookie_name)
        .and_then(|cookie| Some(cookie.value().to_string()));

    if csrf_cookie.is_none() {
        tracing::error!("Cookie '{cookie_name}' is missing!");
        return Err(StatusCode::BAD_REQUEST.into_response());
    }

    Ok(csrf_cookie.unwrap())
}

fn extract_csrf_token_from_form_data(
    form_data: &Vec<(String, String)>,
    form_field_name: &str,
) -> Result<String, Response> {
    let mut csrf_token = None;

    for (key, value) in form_data {
        if form_field_name == key {
            csrf_token = Some(value);
            break;
        }
    }

    let csrf_token = csrf_token.ok_or_else(|| {
        tracing::error!("Missing form field '{form_field_name}' in request!");
        StatusCode::BAD_REQUEST.into_response()
    })?;

    Ok(csrf_token.clone())
}
