use std::{collections::HashMap, path::PathBuf};

use serde::Deserialize;

use crate::{error::MinchirError, Res};

use super::yaml_helper::{self, MergeSettings, SequenceStrategy};

#[derive(Debug, Deserialize, PartialEq)]
pub struct Scopes {
    scopes: HashMap<String, Vec<String>>,
}

impl Scopes {
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
        for (k, v) in merged {
            let k = String::from(k.as_str().unwrap());

            if !v.is_sequence() {
                return Err(MinchirError::YamlScopesKeyHasNoSequenceError { key: k });
            }

            let mut items = Vec::new();

            for seq_entry in v.as_sequence().unwrap() {
                if !seq_entry.is_string() {
                    return Err(MinchirError::YamlScopesKeyHasNonStringItemError { key: k });
                }

                items.push(String::from(seq_entry.as_str().unwrap()));
            }
            map.insert(k, items);
        }

        Ok(Scopes { scopes: map })
    }
}

#[cfg(test)]
mod tests {
    use std::{collections::HashMap, path::PathBuf};

    use crate::{settings::scopes::Scopes, Res};

    #[test]
    fn unit_should_load_and_merge_scopes() -> Res<()> {
        let expected = Scopes {
            scopes: HashMap::from([
                (
                    String::from("scopes3"),
                    vec![
                        String::from("one"),
                        String::from("two"),
                        String::from("three"),
                    ],
                ),
                (
                    String::from("scopes1"),
                    vec![
                        String::from("three"),
                        String::from("four"),
                        String::from("five"),
                    ],
                ),
                (
                    String::from("scopes2"),
                    vec![
                        String::from("one"),
                        String::from("two"),
                        String::from("three"),
                    ],
                ),
            ]),
        };

        let scopes = Scopes::load(&vec![
            PathBuf::from("resources/settings/scopes/scopes1.yaml"),
            PathBuf::from("resources/settings/scopes/scopes2.yaml"),
            PathBuf::from("resources/settings/scopes/scopes3.yaml"),
        ])?;

        assert_eq!(scopes, expected);

        Ok(())
    }
}
