use axum::{
    async_trait,
    extract::{FromRequest, RequestParts},
};
use http::{header, StatusCode};
use unic_langid::LanguageIdentifier;

pub(crate) struct AcceptLanguage(pub Option<String>);

#[async_trait]
impl<B> FromRequest<B> for AcceptLanguage
where
    B: Send,
{
    type Rejection = StatusCode;

    async fn from_request(req: &mut RequestParts<B>) -> Result<Self, Self::Rejection> {
        let header = req.headers().get(header::ACCEPT_LANGUAGE);
        if header.is_none() {
            return Ok(AcceptLanguage(None));
        }
        let header = header.unwrap().to_str().map_err(|error| {
            tracing::error!("{error:?}");
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

        let parsed = accept_language::parse(header);

        if parsed.is_empty() {
            return Ok(AcceptLanguage(None));
        }

        let lang = parsed[0].parse::<LanguageIdentifier>().map_err(|error| {
            tracing::error!("{error:?}");
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

        let lang = if lang.region.is_none() {
            lang.language.as_str().to_string()
        } else {
            format!("{}-{}", lang.language.as_str(), lang.region.unwrap().as_str())
        };

        Ok(AcceptLanguage(Some(lang)))
    }
}
