use std::path::PathBuf;

use serde_yaml::Value;

use crate::{error::MinchirError, Res};

pub enum SequenceStrategy {
    Add,
    Replace,
}

pub struct MergeSettings {
    pub seq_strat: SequenceStrategy,
}

pub fn load_yamls(files: &Vec<PathBuf>, settings: MergeSettings) -> Res<Value> {
    if files.len() < 1 {
        panic!("No files to parse!");
    }

    let mut yamls = Vec::new();
    for file in files {
        yamls.push(load_yaml(file)?);
    }

    let mut base = yamls.remove(0);

    for (idx, overlay) in yamls.iter().enumerate() {
        merge_values(
            &files[0],
            "<root>",
            &mut base,
            &files[idx + 1],
            "<root>",
            overlay,
            &settings,
        )?;
    }

    Ok(base)
}

fn load_yaml(file: &PathBuf) -> Res<Value> {
    let f = std::fs::File::open(file)?;
    Ok(serde_yaml::from_reader(f)?)
}

fn merge_values(
    lh_file: &PathBuf,
    lh_key: &str,
    lh_value: &mut Value,
    rh_file: &PathBuf,
    rh_key: &str,
    rh_value: &Value,
    settings: &MergeSettings,
) -> Res<()> {
    if rh_value.is_null() {
        return Ok(());
    }
    if lh_value.is_null() {
        *lh_value = rh_value.clone();
        return Ok(());
    }

    if !lh_value.is_sequence() && rh_value.is_sequence() {
        *lh_value = rh_value.clone();
        return Ok(());
    }

    if !lh_value.is_mapping() && rh_value.is_mapping() {
        *lh_value = rh_value.clone();
        return Ok(());
    }

    if lh_value.is_sequence() && rh_value.is_sequence() {
        match settings.seq_strat {
            SequenceStrategy::Replace => *lh_value = rh_value.clone(),
            SequenceStrategy::Add => {
                let ls = lh_value.as_sequence_mut().unwrap();
                let rs = rh_value.as_sequence().unwrap();

                for v in rs {
                    ls.push(v.clone());
                }
            }
        }
        return Ok(());
    }

    if lh_value.is_mapping() && rh_value.is_mapping() {
        let lm = lh_value.as_mapping_mut().unwrap();
        let rm = rh_value.as_mapping().unwrap();

        for (k, v) in rm {
            if lm.contains_key(k) {
                let lh_key = &format!("{}.{}", lh_key, k.as_str().unwrap());
                let rh_key = &format!("{}.{}", rh_key, k.as_str().unwrap());

                merge_values(
                    lh_file,
                    lh_key,
                    lm.get_mut(k).unwrap(),
                    rh_file,
                    rh_key,
                    v,
                    settings,
                )?;
            } else {
                lm.insert(k.clone(), v.clone());
            }
        }
        return Ok(());
    }

    if lh_value.is_sequence() && !rh_value.is_sequence() {
        return Err(MinchirError::YamlMergeError {
            lh_type: String::from("sequence"),
            lh_name: String::from(lh_key),
            lh_file: String::from(lh_file.to_str().unwrap()),
            rh_name: String::from(rh_key),
            rh_file: String::from(rh_file.to_str().unwrap()),
        });
    }

    *lh_value = rh_value.clone();

    return Ok(());
}

#[cfg(test)]
mod tests {
    use std::path::PathBuf;

    use serde_yaml::Value;

    use crate::{
        settings::yaml_helper::{load_yamls, MergeSettings, SequenceStrategy},
        Res,
    };

    #[test]
    fn unit_should_merge_four_yaml_files() -> Res<()> {
        let expected = r#"
            eins:
              value: 1
              present: true
              is_null: null
            zwei: 2.0
            drei:
              - "a"
              - "b"
              - "x"
              - "y"
        "#;

        let expected: Value = serde_yaml::from_str(expected)?;

        let yamls = load_yamls(
            &vec![
                PathBuf::from("resources/settings/yaml_helper/merge_yaml_01.yaml"),
                PathBuf::from("resources/settings/yaml_helper/merge_yaml_02.yaml"),
                PathBuf::from("resources/settings/yaml_helper/merge_yaml_03.yaml"),
                PathBuf::from("resources/settings/yaml_helper/merge_yaml_04.yaml"),
            ],
            MergeSettings {
                seq_strat: SequenceStrategy::Add,
            },
        )?;

        assert_eq!(yamls, expected);

        Ok(())
    }
}
