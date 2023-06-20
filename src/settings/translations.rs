use super::yaml_helper::{self, MergeSettings, SequenceStrategy};
use crate::{error::MinchirError, Res};
use serde::Deserialize;
use std::{collections::HashMap, path::PathBuf};
use unic_langid::LanguageIdentifier;

#[derive(Debug, Deserialize, PartialEq, Clone)]
pub struct Translations {
    languages: HashMap<String, String>,
    n_a: String,
}

impl Translations {
    pub fn load(files: &Vec<PathBuf>) -> Res<Self> {
        let merged = yaml_helper::load_yamls(
            files,
            MergeSettings {
                seq_strat: SequenceStrategy::Replace,
            },
        )?;

        if !merged.is_mapping() {
            return Err(MinchirError::YamlScopesNotAMapError);
        }

        let mut map = HashMap::new();

        let merged = merged.as_mapping().unwrap();
        for (lang, translations) in merged {
            let lang = String::from(lang.as_str().unwrap());

            if !translations.is_mapping() {
                return Err(MinchirError::YamlTranslationsKeyHasNoMapError { key: lang });
            }

            for (k, v) in translations.as_mapping().unwrap() {
                if !v.is_string() {
                    return Err(MinchirError::YamlTranslationsKeyHasNonStringMapError {
                        key: lang,
                    });
                }

                map.insert(
                    format!("{}/{}", lang, k.as_str().unwrap()),
                    String::from(v.as_str().unwrap()),
                );
            }
        }

        Ok(Translations {
            languages: map,
            n_a: "N/A".to_string(),
        })
    }

    pub(crate) fn lookup(
        &self,
        key: &str,
        lang_id: &LanguageIdentifier,
        default_lang: &str,
    ) -> &str {
        let lang = lang_id.language.as_str();

        if let Some(region) = lang_id.region {
            let lang_and_key = format!("{}-{}/{}", lang, region.as_str(), key);
            let value = self.languages.get(&lang_and_key);
            if value.is_some() {
                return value.unwrap();
            }
        }

        let lang_and_key = format!("{}/{}", lang, key);
        let value = self.languages.get(&lang_and_key);
        if value.is_some() {
            return value.unwrap();
        }

        let lang_and_key = format!("{}/{}", default_lang, key);
        let value = self.languages.get(&lang_and_key);
        if value.is_some() {
            return value.unwrap();
        }

        &self.n_a
    }
}

#[cfg(test)]
mod tests {
    use std::{collections::HashMap, path::PathBuf};

    use crate::{settings::translations::Translations, Res};

    #[test]
    fn unit_should_load_and_merge_translations() -> Res<()> {
        let expected = Translations {
            languages: HashMap::from([
                (String::from("de/eins"), String::from("translations1")),
                (String::from("de/zwei"), String::from("translations3")),
                (String::from("global/glob1"), String::from("translations1")),
                (String::from("global/glob2"), String::from("translations2")),
                (String::from("pl/jeden"), String::from("translations3")),
                (String::from("fr/un"), String::from("translations2")),
                (String::from("fr/deux"), String::from("translations3")),
                (String::from("en/four"), String::from("translations2")),
                (String::from("en/one"), String::from("translations1")),
                (String::from("en/two"), String::from("translations2")),
                (String::from("en/three"), String::from("translations1")),
            ]),
            n_a: "N/A".to_string(),
        };

        let translations = Translations::load(&vec![
            PathBuf::from("resources/settings/i18n/translations1.yaml"),
            PathBuf::from("resources/settings/i18n/translations2.yaml"),
            PathBuf::from("resources/settings/i18n/translations3.yaml"),
        ])?;

        assert_eq!(translations, expected);

        Ok(())
    }
}
