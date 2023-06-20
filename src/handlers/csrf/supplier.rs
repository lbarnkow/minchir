use std::time::{SystemTime, UNIX_EPOCH};

use hmac::{Mac, SimpleHmac};
use rand::RngCore;
use sha2::Sha512;
use totp_rs::{Algorithm, Secret, TotpUrlError, TOTP};

use crate::{
    error::MinchirError,
    settings::config::{self},
    Res,
};

use super::CsrfConfig;

const ALGORITHM: Algorithm = Algorithm::SHA512;
const BASE32_ALPHABET: base32::Alphabet = base32::Alphabet::RFC4648 { padding: false };
const TOTP_LENGTH: usize = 8;
const TOTP_SKEW_STEPS: u8 = 1;

type HmacSha512 = SimpleHmac<Sha512>;

#[derive(Clone)]
pub struct CsrfSupplier {
    totp: TOTP,
    hmac: HmacSha512,
}

impl CsrfSupplier {
    pub fn build(config: &CsrfConfig) -> Res<Self> {
        let totp = CsrfSupplier::init_totp(config)?;
        let hmac = CsrfSupplier::init_hmac(config)?;

        Ok(CsrfSupplier { totp, hmac })
    }

    pub fn generate(&self) -> Res<(String, String)> {
        self.generate_at(&SystemTime::now())
    }

    fn generate_at(&self, ts: &SystemTime) -> Res<(String, String)> {
        let ts = Self::systemtime_as_unixtimestamp(&ts)?;
        let totp = self.totp.generate(ts);

        let mut hmac = self.hmac.clone();
        hmac.update(totp.as_bytes());
        let hmac = hmac.finalize().into_bytes();
        let hmac = hmac.as_slice();
        let hmac = base32::encode(BASE32_ALPHABET, hmac);

        Ok((totp, hmac))
    }

    pub fn verify(&self, totp_token: &str, hmac_code: &str) -> Res<()> {
        self.verify_at(totp_token, hmac_code, &SystemTime::now())
    }

    fn verify_at(&self, totp_token: &str, enc_hmac_code: &str, ts: &SystemTime) -> Res<()> {
        let ts = Self::systemtime_as_unixtimestamp(ts)?;
        let valid = self.totp.check(totp_token, ts);
        if !valid {
            return Err(MinchirError::TotpVerificationError);
        }

        let hmac_code = base32::decode(BASE32_ALPHABET, enc_hmac_code);
        if hmac_code.is_none() {
            return Err(MinchirError::HmacBase32DecodingError {
                encoded: enc_hmac_code.to_string(),
            });
        }

        let hmac_code = hmac_code.unwrap();
        let hmac_code = hmac_code.as_slice();

        let mut hmac = self.hmac.clone();
        hmac.update(totp_token.as_bytes());
        hmac.verify_slice(hmac_code)?;

        Ok(())
    }

    fn init_hmac(config: &CsrfConfig) -> Res<HmacSha512> {
        let hmac_key: Vec<u8>;

        if let Some(k) = &config.hmac_key {
            hmac_key = Secret::Encoded(k.clone()).to_bytes().unwrap();
        } else {
            hmac_key = CsrfSupplier::random_key_vec(config::TOTP_MIN_KEY_LENGTH_BITS);
        }

        let hmac_key = hmac_key.as_slice();
        Ok(HmacSha512::new_from_slice(hmac_key).expect("HMAC can take key of any size"))
    }

    fn systemtime_as_unixtimestamp(ts: &SystemTime) -> Res<u64> {
        let t = ts.duration_since(UNIX_EPOCH)?.as_secs();
        Ok(t)
    }

    fn init_totp(config: &CsrfConfig) -> Res<TOTP> {
        let totp_key: Vec<u8>;

        if let Some(k) = &config.totp_key {
            totp_key = Secret::Encoded(k.clone()).to_bytes().unwrap();
        } else {
            totp_key = CsrfSupplier::random_key_vec(config::TOTP_MIN_KEY_LENGTH_BITS);
        }

        Ok(TOTP::new(
            ALGORITHM,
            TOTP_LENGTH,
            TOTP_SKEW_STEPS,
            config.totp_ttl_s,
            totp_key,
        )?)
    }

    fn random_key_vec(size_bits: usize) -> Vec<u8> {
        let mut key = vec![0; size_bits / 8];
        let mut buf = key.as_mut_slice();
        rand::thread_rng().fill_bytes(&mut buf);
        key
    }
}

impl From<TotpUrlError> for MinchirError {
    fn from(e: TotpUrlError) -> Self {
        MinchirError::TotpUrlError { msg: e.to_string() }
    }
}

#[cfg(test)]
mod tests {
    use rstest::rstest;

    use std::time::{Duration, SystemTime};

    use crate::{
        error::MinchirError,
        settings::config::{self},
        Res,
    };

    use super::CsrfSupplier;

    #[rstest]
    #[case(
        "D2GXOZOMIQWGBVDYKSTHDSSIUN4PJYEQJHWWZ7WRR74ZVK44XAFCBKXLEFTS4AQ7TW7SOGASASU7PWACUAJJRRGD57PABBEY2UIHUVA",
        "756RWGP4FYARJ52O2ZSDVARXTMGPEVDXV54UC22HUZFO26RUBTTVDAS57EQ5WQBL4SLXNNCLCX5KME7OY44I2UGWJDQQSQJRYHGISUY",
        801231316u64,
        "48030610",
        "EEFQVS25ZW72BHINSZSYSLDVF7QNFTRD32KNBTRXL5Y33UC5PXW6B5GD6O7JBPUVJT4XMASEDHLCTXA7CZ3LEBY3ORQWSMXMVNYMAHY",
        "61179155",
        "BU4KAOEGBCRBUBVVDGAPJV3552QOPXB6UHVOUTQQQIDZKMHWMHFGLKDNB2F4QSYVHK5WEODPU25R3LLKIOIMMXDXJDHRFYWQM6BLMDY",
    )]
    #[case(
        "3K5DYHK3F6M3JVPWMNZ75ZH4C2WCSQ77X3IZCIBWASCTLD5BBNQ5SQIEPAOPFNH467LJ2KEUY5OD4DF3TVVJ5DJLXXGMFILFLWBL6ZA",
        "3G7B7FJAW62GGMVX63RLKCSLRZ2JPYGIDZ6MH6RWUCVH2I6TCG3GNTY5DKYSQIXF4GHHWEKCVBSXCPXRCISE4KRT524ELBFZVCUAHPQ",
        1116591316u64,
        "57228566",
        "KRGSIAIGHNPQ2CT2QLTAARWTKNMTTAVM3RKQQ4QS27BH6XXOSENFIH7ZXWAZUY44JAP4ZGHE5WLBQHM2OEKZ3WMUUPXYMT6TI62BMQQ",
        "28797404",
        "TOTAG54P7OKJFFDXY2AVGVT524ELF4YH5KIBAI7236HE74BDFUURVDKWYIPDCV7XXNVYIQHZA3ULGWPTWZBOUJURJ2ICTYU5OEGWKNY",
    )]
    #[case(
        "SNNP3SYNRHKHM6B3MMGGSTGCVUBK4PI7TVIDTG7OAPSLASSHPGFGRZTXJIEYVKYGP5S7Z66BWWPOMWKYJHDRN2LYNXFSBF7PEMXD4PI",
        "BY52DF2VH556XQO4J44FMJW3QN7QD7GU4WJJWXTPQDQQRK5HSIT4UIHFRI7MF3WFWSPK75WAAJQIWJ3LRFKFYRCW4NEC7DBPKGG25DQ",
        1431951316u64,
        "44586690",
        "PNFW6SM2ZQDFJUX6RQI5YFVYVZHOVA3JVEC5QWH6DN33K5MXGHN6536TXDXVGOTRBL54XAWFX6QROUYFKJ6BD5YEQYIVISC5MKEPWHQ",
        "45287569",
        "PUJU3NESDW3GV5OIFCZXI7VXRRKT6SVBNGM6HUXC6GZZG3DXPS2MWGNFF4THMB5P7NGQ3XGDX3RKHGAUDJHS64DQDVFQNYJSQHB6C5A",
    )]
    fn unit_should_generate_expected_tokens(
        #[case] totp_secret: &str,
        #[case] hmac_secret: &str,
        #[case] ts: u64,
        #[case] totp_current: &str,
        #[case] hmac_current: &str,
        #[case] totp_prev: &str,
        #[case] hmac_prev: &str,
    ) -> Res<()> {
        let ts = SystemTime::UNIX_EPOCH
            .checked_add(Duration::from_secs(ts))
            .unwrap();

        let csrf = config::Csrf::new(
            300,
            Some(totp_secret.to_string()),
            Some(hmac_secret.to_string()),
        );
        let cfg = build_config(csrf);
        let sup = CsrfSupplier::build(&cfg.into())?;

        let (t, h) = sup.generate_at(&ts)?;
        assert_eq!(t, totp_current);
        assert_eq!(h, hmac_current);

        let ts = ts.checked_sub(Duration::from_secs(300)).unwrap();
        let (t, h) = sup.generate_at(&ts)?;
        assert_eq!(t, totp_prev);
        assert_eq!(h, hmac_prev);

        Ok(())
    }

    #[rstest]
    #[case(
        "D2GXOZOMIQWGBVDYKSTHDSSIUN4PJYEQJHWWZ7WRR74ZVK44XAFCBKXLEFTS4AQ7TW7SOGASASU7PWACUAJJRRGD57PABBEY2UIHUVA",
        "756RWGP4FYARJ52O2ZSDVARXTMGPEVDXV54UC22HUZFO26RUBTTVDAS57EQ5WQBL4SLXNNCLCX5KME7OY44I2UGWJDQQSQJRYHGISUY",
        801231316u64,
        "48030610",
        "EEFQVS25ZW72BHINSZSYSLDVF7QNFTRD32KNBTRXL5Y33UC5PXW6B5GD6O7JBPUVJT4XMASEDHLCTXA7CZ3LEBY3ORQWSMXMVNYMAHY",
        "61179155",
        "BU4KAOEGBCRBUBVVDGAPJV3552QOPXB6UHVOUTQQQIDZKMHWMHFGLKDNB2F4QSYVHK5WEODPU25R3LLKIOIMMXDXJDHRFYWQM6BLMDY",
    )]
    #[case(
        "3K5DYHK3F6M3JVPWMNZ75ZH4C2WCSQ77X3IZCIBWASCTLD5BBNQ5SQIEPAOPFNH467LJ2KEUY5OD4DF3TVVJ5DJLXXGMFILFLWBL6ZA",
        "3G7B7FJAW62GGMVX63RLKCSLRZ2JPYGIDZ6MH6RWUCVH2I6TCG3GNTY5DKYSQIXF4GHHWEKCVBSXCPXRCISE4KRT524ELBFZVCUAHPQ",
        1116591316u64,
        "57228566",
        "KRGSIAIGHNPQ2CT2QLTAARWTKNMTTAVM3RKQQ4QS27BH6XXOSENFIH7ZXWAZUY44JAP4ZGHE5WLBQHM2OEKZ3WMUUPXYMT6TI62BMQQ",
        "28797404",
        "TOTAG54P7OKJFFDXY2AVGVT524ELF4YH5KIBAI7236HE74BDFUURVDKWYIPDCV7XXNVYIQHZA3ULGWPTWZBOUJURJ2ICTYU5OEGWKNY",
    )]
    #[case(
        "SNNP3SYNRHKHM6B3MMGGSTGCVUBK4PI7TVIDTG7OAPSLASSHPGFGRZTXJIEYVKYGP5S7Z66BWWPOMWKYJHDRN2LYNXFSBF7PEMXD4PI",
        "BY52DF2VH556XQO4J44FMJW3QN7QD7GU4WJJWXTPQDQQRK5HSIT4UIHFRI7MF3WFWSPK75WAAJQIWJ3LRFKFYRCW4NEC7DBPKGG25DQ",
        1431951316u64,
        "44586690",
        "PNFW6SM2ZQDFJUX6RQI5YFVYVZHOVA3JVEC5QWH6DN33K5MXGHN6536TXDXVGOTRBL54XAWFX6QROUYFKJ6BD5YEQYIVISC5MKEPWHQ",
        "45287569",
        "PUJU3NESDW3GV5OIFCZXI7VXRRKT6SVBNGM6HUXC6GZZG3DXPS2MWGNFF4THMB5P7NGQ3XGDX3RKHGAUDJHS64DQDVFQNYJSQHB6C5A",
    )]
    fn unit_should_verify_given_tokens(
        #[case] totp_secret: &str,
        #[case] hmac_secret: &str,
        #[case] ts: u64,
        #[case] totp_current: &str,
        #[case] hmac_current: &str,
        #[case] totp_prev: &str,
        #[case] hmac_prev: &str,
    ) -> Res<()> {
        let ts = SystemTime::UNIX_EPOCH
            .checked_add(Duration::from_secs(ts))
            .unwrap();

        let csrf = config::Csrf::new(
            300,
            Some(totp_secret.to_string()),
            Some(hmac_secret.to_string()),
        );
        let cfg = build_config(csrf);

        let sup = CsrfSupplier::build(&cfg.into())?;

        sup.verify_at(totp_current, hmac_current, &ts)?;
        sup.verify_at(totp_prev, hmac_prev, &ts)?;

        Ok(())
    }

    #[test]
    fn unit_should_generate_and_verify_with_random_keys() -> Res<()> {
        let csrf = config::Csrf::new(300, None, None);
        let cfg = build_config(csrf);
        let sup = CsrfSupplier::build(&cfg.into())?;

        let (totp_token, hmac_code) = sup.generate()?;

        sup.verify(&totp_token, &hmac_code)?;

        Ok(())
    }

    #[test]
    fn unit_should_reject_random_totp_token() -> Res<()> {
        let csrf = config::Csrf::new(300, None, None);
        let cfg = build_config(csrf);
        let sup = CsrfSupplier::build(&cfg.into())?;

        let (_, hmac_code) = sup.generate()?;
        let totp_token = "123456";

        let result = sup.verify(totp_token, &hmac_code);
        let error = result.expect_err("Should have failed to verify totp");

        if let MinchirError::TotpVerificationError = error {
            return Ok(());
        }

        panic!("Should have returned a totp verification error!");
    }

    #[test]
    fn unit_should_reject_non_base_32_hmac_cookie() -> Res<()> {
        let csrf = config::Csrf::new(300, None, None);
        let cfg = build_config(csrf);
        let sup = CsrfSupplier::build(&cfg.into())?;

        let (totp_token, _) = sup.generate()?;
        let hmac_code = "lowercase-not-in-alphabet";

        let result = sup.verify(&totp_token, hmac_code);
        let error = result.expect_err("Should have failed to decode");

        if let MinchirError::HmacBase32DecodingError { encoded: _ } = error {
            return Ok(());
        }

        panic!("Should have returned a base32 decoding error!");
    }

    #[test]
    fn unit_should_reject_random_hmac_cookie() -> Res<()> {
        let csrf = config::Csrf::new(300, None, None);
        let cfg = build_config(csrf);
        let sup = CsrfSupplier::build(&cfg.into())?;

        let (totp_token, hmac_code) = sup.generate()?;
        let hmac_code = format!("{}{}", "AAAA", &hmac_code[4..]);

        let result = sup.verify(&totp_token, &hmac_code);
        let error = result.expect_err("Should have failed to verify hmac");

        if let MinchirError::HmacVerificationError { source: _ } = error {
            return Ok(());
        }

        panic!("Should have returned a hmac verification error!");
    }

    fn build_config(csrf: config::Csrf) -> config::Config {
        let cookies = config::Cookies::new(true, true, "/".to_string());
        let server = config::Server::new(8080, "".to_string(), cookies);
        config::Config::new(
            server,
            csrf,
            config::Hydra::default(),
            config::Ldap::default(),
        )
    }
}
