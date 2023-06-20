use self::{config::Config, scopes::Scopes, translations::Translations};

pub mod config;
pub mod scopes;
pub mod translations;
mod yaml_helper;

#[derive(Debug)]
pub struct Settings {
    pub config: Config,
    pub scopes: Scopes,
    pub translations: Translations,
}
