use std::{fs::File, io::BufReader, sync::Arc, time::Duration};

use axum::{
    async_trait,
    extract::{FromRequest, RequestParts},
    response::{IntoResponse, Response},
    Extension,
};
use deadpool::managed::Object;
use deadpool_ldap::{Manager, Pool};
use http::StatusCode;
use ldap3::LdapConnSettings;
use rustls::{client::WebPkiVerifier, ClientConfig, RootCertStore};

use crate::{error::MinchirError, settings::config::Config, Res};

#[derive(Clone)]
pub struct LdapLayer {
    pool: Arc<Pool>,
}

type Client = Object<Manager>;

impl LdapLayer {
    pub fn new(cfg: Config) -> Res<Extension<Self>> {
        let cfg = cfg.ldap();

        let mut con_settings = LdapConnSettings::new().set_conn_timeout(Duration::from_secs(60));

        if cfg.ca_bundle().is_some() {
            let file = File::open(cfg.ca_bundle().as_ref().unwrap().clone())
                .map_err(|error| MinchirError::LdapClientCaBundleError { source: error })?;
            let mut reader = BufReader::new(file);
            let certs = rustls_pemfile::certs(&mut reader)
                .map_err(|error| MinchirError::LdapClientCaBundleError { source: error })?;

            let mut roots = RootCertStore::empty();
            roots.add_parsable_certificates(&certs);

            let verifier = Arc::new(WebPkiVerifier::new(roots, None));

            let client_config = Arc::new(
                ClientConfig::builder()
                    .with_safe_defaults()
                    .with_custom_certificate_verifier(verifier)
                    .with_no_client_auth(),
            );

            con_settings = con_settings.set_config(client_config);
        }

        if *cfg.skip_tls_verification() {
            tracing::warn!(
                "skipping certificate verification when accessing ldap server is very insecure!"
            );
            con_settings = con_settings.set_no_tls_verify(*cfg.skip_tls_verification());
        }

        let manager = Manager::new(cfg.server_url()).with_connection_settings(con_settings);

        let pool = Pool::builder(manager)
            .max_size(5)
            .build()
            .map_err(|error| MinchirError::LdapPoolBuildError {
                msg: error.to_string(),
            })?;

        Ok(Extension(Self {
            pool: Arc::new(pool),
        }))
    }

    pub async fn get_connection(&self) -> Res<Client> {
        Ok(self
            .pool
            .get()
            .await
            .map_err(|error| MinchirError::LdapPoolAccessError {
                msg: error.to_string(),
            })?)
    }
}

#[async_trait]
impl<B> FromRequest<B> for LdapLayer
where
    Self: Send + Sync + 'static,
    B: Send,
{
    type Rejection = Response;

    async fn from_request(req: &mut RequestParts<B>) -> Result<Self, Self::Rejection> {
        let ldap_layer = req.extensions().get::<LdapLayer>().ok_or_else(|| {
            tracing::error!("Extension 'LdapLayer' is missing!");
            StatusCode::INTERNAL_SERVER_ERROR.into_response()
        })?;

        Ok(ldap_layer.clone())
    }
}
