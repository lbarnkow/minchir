use std::sync::{Arc, Mutex};

use ::minchir::error::MinchirError;
use thiserror::Error;

use self::minchir::MinchirClient;

// pub mod browser;
pub mod hydra;
pub mod minchir;
// pub mod page;

// struct Bla {
//     feld: u32,
// }

// lazy_static::lazy_static! {
//     static ref BLA: Arc<Mutex<Bla>> = Arc::new(Mutex::new(Bla { feld: 0u32 }));
// }

#[derive(Clone, Copy)]
pub struct TestConfig {
    pub ory_url: &'static str,
    pub ory_admin_url: &'static str,
    pub minchir_url: &'static str,
    pub client_id: &'static str,
    pub login_callback: &'static str,
    pub logout_callback: &'static str,
    pub oidc_scope: &'static str,
    pub oidc_state: &'static str,
    pub oidc_response_type: &'static str,
}

impl TestConfig {
    pub fn new() -> Self {
        TestConfig {
            ory_url: "https://localhost:14444",
            ory_admin_url: "https://localhost:14445",
            minchir_url: "http://localhost:18080",
            client_id: "oidc-client-1",
            login_callback: "http://localhost:28080/",
            logout_callback: "http://localhost:28080/",
            oidc_scope: "openid",
            oidc_state: "f69ebbf6-c43b-4e3a-b7af-63a4a9dc09ea",
            oidc_response_type: "code",
        }
    }
}

pub fn setup() -> TRes<MinchirClient> {
    // {
    //     let my = BLA.clone();
    //     let mut guard = my.lock().unwrap();
    //     guard.feld += 1;

    //     println!("{}", guard.feld);
    //     println!("{}", guard.feld);
    //     println!("{}", guard.feld);
    //     println!("{}", guard.feld);
    //     println!("{}", guard.feld);
    //     println!("{}", guard.feld);
    // }

    let conf = TestConfig::new();
    hydra::clean_consent_sessions(conf)?;
    let minchir = MinchirClient::build(conf)?;

    Ok(minchir)
}

pub type TRes<T> = Result<T, MinchirTestError>;

#[derive(Debug, Error)]
pub enum MinchirTestError {
    #[error("{source}")]
    LibNoBrowserBrowserError {
        #[from]
        source: no_browser::browser::Error,
    },

    #[error("{source}")]
    LibNoBrowserPageError {
        #[from]
        source: no_browser::page::Error,
    },

    #[error("{source}")]
    LibNoBrowserFormError {
        #[from]
        source: no_browser::form::Error,
    },

    #[error("{source}")]
    HyperError {
        #[from]
        source: hyper::Error,
    },

    #[error("{source}")]
    MinchirError {
        #[from]
        source: MinchirError,
    },

    #[error("Generic reqwest error!")]
    ReqwestError {
        #[from]
        source: reqwest::Error,
    },

    #[error("{prop} '{lhs}' {reason}!")]
    UnaryPropComparisonError {
        prop: String,
        reason: String,
        lhs: String,
    },
    #[error("{prop} '{lhs}' {reason} '{rhs}'!")]
    BinaryPropComparisonError {
        prop: String,
        reason: String,
        lhs: String,
        rhs: String,
    },
}
