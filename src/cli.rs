use std::path::PathBuf;

use clap::Parser;
use derive_getters::Getters;
use derive_new::new;

/// Minchir is a login and consent app for ORY Hydra backed by your existing
/// LDAP infrastructure. Its UI supports easy customization via templates and
/// internationalization for different languages.
#[derive(Parser, Debug, Getters, new)]
#[clap(author = "https://github.com/lbarnkow/minchir/", version)]
pub struct Args {
    /// A configuration file to load. At least one configuration file must
    /// be specified! This parameter may be used mutliple times, in which case all
    /// configuration files will be merged in to one configuration with latter files
    /// overriding settings in former files.
    #[clap(
        id = "config",
        short,
        long,
        value_parser,
        value_name = "FILE",
        required = true
    )]
    configs: Vec<PathBuf>,

    /// A translation file to load. At least one translation file must
    /// be specified! This parameter may be used mutliple times, in which case all
    /// translation files will be merged in to one translation with latter files
    /// overriding translations in former files.
    #[clap(short, long, value_parser, value_name = "FILE", required = true)]
    translations: Vec<PathBuf>,

    /// A scope file to load. At least one scope file must
    /// be specified! This parameter may be used mutliple times, in which case all
    /// scope files will be merged in to one scope with latter files
    /// overriding scopes in former files.
    #[clap(short, long, value_parser, value_name = "FILE", required = true)]
    scopes: Vec<PathBuf>,
}
