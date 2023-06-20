use thiserror::Error;

#[derive(Debug, Error)]
pub enum MinchirError {
    #[error("{msg}")]
    GenericError { msg: String },

    #[error("Failed to load yaml file! {}", source.to_string())]
    YamlIoError {
        #[from]
        source: std::io::Error,
    },

    #[error("Failed to parse yaml file! {}", source.to_string())]
    YamlParseError {
        #[from]
        source: serde_yaml::Error,
    },

    #[error("Cannot merge {lh_type} '{lh_name}' from {lh_file} with '{rh_name} from {rh_file}!")]
    YamlMergeError {
        lh_type: String,
        lh_name: String,
        lh_file: String,
        rh_name: String,
        rh_file: String,
    },

    #[error("Merged scopes files did not result in a map!")]
    YamlScopesNotAMapError,

    #[error("Merged scopes contains key '{key}' that is not a sequence!")]
    YamlScopesKeyHasNoSequenceError { key: String },

    #[error("Merged scopes contains a sequence item under key '{key}' that is not a string!")]
    YamlScopesKeyHasNonStringItemError { key: String },

    #[error("Merged translations files did not result in a map!")]
    YamlTranslationsNotAMapError,

    #[error("Merged translations contains key '{key}' that is not a map!")]
    YamlTranslationsKeyHasNoMapError { key: String },

    #[error("Merged translations contains a map item under key '{key}' that is not entirely made up of strings!")]
    YamlTranslationsKeyHasNonStringMapError { key: String },

    #[error("TOTP key in configuration key size must be between {} bits ({} bytes) and {} bits ({} bytes)!", min_bits, min_bits / 8, max_bits, max_bits / 8)]
    ConfigCsrfTotpKeyValidationError { min_bits: usize, max_bits: usize },

    #[error("TOTP verification failed!")]
    TotpVerificationError,

    #[error("TOTP error: {msg}")]
    TotpUrlError { msg: String },

    #[error("TOTP Error: {}", source.to_string())]
    TotpSystemTimeError {
        #[from]
        source: std::time::SystemTimeError,
    },

    #[error("HMAC base 32 decoding error, encoded string: '{}'!", encoded)]
    HmacBase32DecodingError { encoded: String },

    #[error("HMAC verification failed! Error: {}", source.to_string())]
    HmacVerificationError {
        #[from]
        source: hmac::digest::MacError,
    },

    #[error("Failed to load custom ca bundle for ldap!")]
    LdapClientCaBundleError { source: std::io::Error },

    #[error("Failed to load custom ca bundle for hydra!")]
    HydraApiClientCaBundleError { source: std::io::Error },

    #[error("Failed to build the hydra api client!")]
    HydraApiClientBuildError { source: reqwest::Error },

    #[error("Templating Error: {}", source.to_string())]
    TemplatingError {
        #[from]
        source: tera::Error,
    },

    #[error("Failed to build LDAP connection pool: {msg}!")]
    LdapPoolBuildError { msg: String },

    #[error("Failed to fetch connection from LDAP connection pool: {msg}!")]
    LdapPoolAccessError { msg: String },
}
