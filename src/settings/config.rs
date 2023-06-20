use crate::{
    error::MinchirError,
    handlers::csrf::{CsrfConfig, CsrfConfigBuilder},
    Res,
};
use derive_getters::Getters;
use derive_new::new;
use serde::Deserialize;
use serde_yaml::Value;
use std::path::PathBuf;

use super::yaml_helper::{self, MergeSettings, SequenceStrategy};

// see https://security.stackexchange.com/a/96176
pub const TOTP_MIN_KEY_LENGTH_BITS: usize = 512; // adjusted for SHA512
pub const TOTP_MAX_KEY_LENGTH_BITS: usize = 1024; // adjusted for SHA512

#[derive(Debug, Deserialize, PartialEq, Getters, Default, Clone, new)]
pub struct Config {
    server: Server,
    csrf: Csrf,
    hydra: Hydra,
    ldap: Ldap,
}

#[derive(Debug, Deserialize, PartialEq, Getters, Default, Clone, new)]
pub struct Server {
    port: u16,
    #[serde(alias = "assetsPath")]
    #[getter(skip)]
    assets_path: String,
    cookies: Cookies,
}

impl Server {
    pub fn default_language(&self) -> &str {
        "en"
    }
}

#[derive(Debug, Deserialize, PartialEq, Getters, Default, Clone, new)]
pub struct Cookies {
    secure: bool,
    #[serde(alias = "httpOnly")]
    http_only: bool,
    path: String,
}

#[derive(Debug, Deserialize, PartialEq, Getters, Default, Clone, new)]
pub struct Csrf {
    #[serde(alias = "totpTtlSeconds")]
    totp_ttl_s: u64,
    #[serde(alias = "totpKey")]
    totp_key: Option<String>,
    #[serde(alias = "hmacKey")]
    hmac_key: Option<String>,
}

#[derive(Debug, Deserialize, PartialEq, Getters, Default, Clone)]
pub struct Hydra {
    #[serde(alias = "adminUrl")]
    admin_url: String,
    #[serde(alias = "skipTlsVerification")]
    skip_tls_verification: bool,
    #[serde(alias = "caBundle")]
    ca_bundle: Option<String>,
    #[serde(alias = "timeoutMilliseconds")]
    timeout_ms: u64,
    #[serde(alias = "rememberForSeconds")]
    remember_for_s: u64,
}

#[derive(Debug, Deserialize, PartialEq, Getters, Default, Clone)]
pub struct Ldap {
    #[serde(alias = "serverUrl")]
    server_url: String,
    #[serde(alias = "skipTlsVerification")]
    skip_tls_verification: bool,
    #[serde(alias = "caBundle")]
    ca_bundle: Option<String>,
    #[serde(alias = "bindDn")]
    bind_dn: String,
    #[serde(alias = "bindPassword")]
    bind_password: String,
    #[serde(alias = "userSearchBaseDn")]
    user_search_base_dn: String,
    #[serde(alias = "userSearchObjectClass")]
    user_search_object_class: String,
    #[serde(alias = "userAttributeUid")]
    user_attribute_uid: String,
    #[serde(alias = "userAttributeGivenName")]
    user_attribute_given_name: String,
    #[serde(alias = "userAttributeSurname")]
    user_attribute_surname: String,
    #[serde(alias = "userAttributeMail")]
    user_attribute_mail: String,
}

impl Config {
    pub fn load(files: &Vec<PathBuf>) -> Res<Self> {
        let merged = yaml_helper::load_yamls(
            files,
            MergeSettings {
                seq_strat: SequenceStrategy::Add,
            },
        )?;
        let merged = serde_yaml::to_string::<Value>(&merged)?;

        let mut config: Config = serde_yaml::from_str(&merged)?;
        config.validate()?;
        config.normalize();

        Ok(config)
    }

    fn validate(&self) -> Res<()> {
        if let Some(totp_key) = &self.csrf.totp_key {
            if totp_key.len() < (TOTP_MIN_KEY_LENGTH_BITS / 8)
                || totp_key.len() > (TOTP_MAX_KEY_LENGTH_BITS / 8)
            {
                return Err(MinchirError::ConfigCsrfTotpKeyValidationError {
                    min_bits: TOTP_MIN_KEY_LENGTH_BITS,
                    max_bits: TOTP_MAX_KEY_LENGTH_BITS,
                });
            }
        }

        Ok(())
    }

    fn normalize(&mut self) {
        Config::normalize_path(&mut self.server.assets_path, std::path::MAIN_SEPARATOR);
        Config::normalize_path(&mut self.server.cookies.path, '/');
        Config::normalize_path(&mut self.hydra.admin_url, '/');
    }

    fn normalize_path(path: &mut String, separator: char) {
        while path.ends_with(separator) {
            path.pop();
        }
    }
}

impl Server {
    pub fn assets_path(&self, subfolder: &str) -> PathBuf {
        [&self.assets_path, subfolder].iter().collect()
    }
}

impl From<Config> for CsrfConfig {
    fn from(cfg: Config) -> Self {
        CsrfConfigBuilder::default()
            .totp_ttl_s(cfg.csrf.totp_ttl_s)
            .totp_key(cfg.csrf.totp_key)
            .hmac_key(cfg.csrf.hmac_key)
            .cookie_secure(cfg.server.cookies.secure)
            .cookie_http_only(cfg.server.cookies.http_only)
            .cookie_path(cfg.server.cookies.path)
            .build()
            .unwrap()
    }
}

#[cfg(test)]
mod tests {
    use std::path::PathBuf;

    use crate::{
        error::MinchirError,
        settings::config::{Config, Cookies, Csrf, Hydra, Ldap, Server},
        Res,
    };

    #[test]
    fn unit_should_normalize_paths_in_config() -> Res<()> {
        let expected = Config {
            server: Server {
                port: 8080,
                assets_path: "assets".into(),
                cookies: Cookies {
                    secure: true,
                    http_only: true,
                    path: String::from("/normalized/path"),
                },
            },
            csrf: Csrf {
                totp_ttl_s: 300,
                totp_key: None,
                hmac_key: None,
            },
            hydra: Hydra {
                admin_url: String::from("http://localhost:4445"),
                skip_tls_verification: true,
                ca_bundle: None,
                timeout_ms: 5000u64,
                remember_for_s: 604800u64,
            },
            ldap: Ldap {
                server_url: String::from("ldaps://localhost:636"),
                skip_tls_verification: false,
                ca_bundle: Some("/etc/ssl/cabundle.pem".to_string()),
                bind_dn: String::from("cn=binduser,ou=Users,dc=lbarnkow,dc=github,dc=com"),
                bind_password: String::from("weakpassword"),
                user_search_base_dn: String::from("ou=Users,dc=lbarnkow,dc=github,dc=com"),
                user_search_object_class: String::from("person"),
                user_attribute_uid: String::from("uid"),
                user_attribute_given_name: String::from("givenName"),
                user_attribute_surname: String::from("sn"),
                user_attribute_mail: String::from("mail"),
            },
        };

        let cfg = Config::load(&vec![PathBuf::from(
            "resources/settings/config/normalize_config.yaml",
        )])?;

        assert_eq!(cfg, expected);

        Ok(())
    }

    #[test]
    fn unit_should_fail_to_load_when_missing_attributes() -> Res<()> {
        let cfg = Config::load(&vec![PathBuf::from(
            "resources/settings/config/missing_attr.yaml",
        )]);

        let err = cfg.expect_err("Should have failed to deserialize!");

        match err {
            MinchirError::YamlParseError { source: _ } => (),
            _ => panic!("Should have failed to deserialize!"),
        };

        Ok(())
    }

    #[test]
    fn unit_should_load_and_merge_configs() -> Res<()> {
        let expected = Config {
            server: Server {
                port: 28080,
                assets_path: "assets".into(),
                cookies: Cookies {
                    secure: true,
                    http_only: true,
                    path: String::from(""),
                },
            },
            csrf: Csrf {
                totp_ttl_s: 3000,
                totp_key: None,
                hmac_key: None,
            },
            hydra: Hydra {
                admin_url: String::from("http://localhost:4445"),
                skip_tls_verification: true,
                ca_bundle: None,
                timeout_ms: 5000u64,
                remember_for_s: 604800u64,
            },
            ldap: Ldap {
                server_url: String::from("ldaps://localhost:636"),
                skip_tls_verification: false,
                ca_bundle: Some("/etc/ssl/cabundle.pem".to_string()),
                bind_dn: String::from("cn=binduser,ou=Users,dc=lbarnkow,dc=github,dc=com"),
                bind_password: String::from("password123"),
                user_search_base_dn: String::from("ou=Users,dc=lbarnkow,dc=github,dc=com"),
                user_search_object_class: String::from("person"),
                user_attribute_uid: String::from("uid"),
                user_attribute_given_name: String::from("givenName"),
                user_attribute_surname: String::from("sn"),
                user_attribute_mail: String::from("mail"),
            },
        };

        let cfg = Config::load(&vec![
            PathBuf::from("resources/settings/config/test_config_01.yaml"),
            PathBuf::from("resources/settings/config/test_config_02.yaml"),
            PathBuf::from("resources/settings/config/test_config_03.yaml"),
        ])?;

        assert_eq!(cfg, expected);

        Ok(())
    }
}
