use std::{net::SocketAddr, thread::Thread, time::Duration, path::PathBuf};

use clap::Parser;
use minchir::cli::Args;

use no_browser::InputType;
use reqwest::StatusCode;
use serial_test::serial;
use test_util::TRes;
use tokio::{runtime::Runtime, task::JoinHandle};

mod test_util;

// Tests run in a serial fashion rather than in parallel b/c most tests use the same
// credentials. Thus, if one tests checks the remember consent option it gets stored
// by ory hydra for all tests as this information is associated with the user.
// Each test starts of by removing all prior consents for the test user.
#[test]
#[serial]
fn int_should_login_and_consent_and_logout() -> TRes<()> {
    let minchir = test_util::setup()?;

    let page = minchir.start_login_flow()?;
    let page = minchir.submit_login_form(
        &page,
        Some(String::from("janedoe")),
        Some(String::from("12345")),
        Some(String::from("")),
        true,
        true,
    )?;
    let page = minchir.submit_consent_form(&page, true, true)?;
    minchir.assert_logged_in_and_redirected(&page)?;

    let page = minchir.start_logout_flow()?;
    let page = minchir.submit_logout_form(&page, true)?;
    minchir.assert_logged_out_and_redirected(&page)?;

    Ok(())
}

#[tokio::test]
#[serial]
async fn int_lorenz() -> TRes<()> {
    // // tokio_test::block_on(future)
    // let rt = Runtime::new().unwrap();

    // tokio::spawn(async {
        // panic!("{:?}", std::env::current_dir());

        let args = Args::new(
            vec![PathBuf::from("./resources/settings/config/integTest/config.yaml")],
            vec![PathBuf::from("./assets/i18n/translations.yaml")],
            vec![PathBuf::from("./assets/scopes/scopes.yaml")]);

        let app = minchir::minchir(&args)?;
        let addr = format!("0.0.0.0:{}", app.settings.config.server().port())
            .parse::<SocketAddr>()
            .unwrap();

        let srv = axum::Server::try_bind(&addr)?;

        srv.serve(app.router.into_make_service()).await?;

        Ok(())
    // });

    // // wait for server to be up.
    // // TODO: wait smarter
    // println!("{:?}", x.is_finished());
    // std::thread::sleep(Duration::from_secs(2));

    // // do request

    // // shutdown server
    // println!("{:?}", x.is_finished());
    // x.abort();

    // println!("{:?}", x.is_finished());
    // std::thread::sleep(Duration::from_secs(2));

    // println!("{:?}", x.is_finished());
    // panic!(".......................");

    // Ok(())
}

#[test]
#[serial]
fn int_should_cancel_login() -> TRes<()> {
    let minchir = test_util::setup()?;

    let page = minchir.start_login_flow()?;
    let page = minchir.submit_login_form(
        &page,
        Some(String::from("")),
        Some(String::from("")),
        Some(String::from("")),
        false,
        false,
    )?;
    minchir.assert_cancelled_login(&page)?;

    Ok(())
}

#[test]
#[serial]
fn int_should_fail_login_with_wrong_credentials() -> TRes<()> {
    let minchir = test_util::setup()?;

    let page = minchir.start_login_flow()?;
    let page = minchir.submit_login_form(
        &page,
        Some(String::from("janedoe")),
        Some(String::from("wrongpwd")),
        Some(String::from("wrongpwd")),
        true,
        true,
    )?;
    minchir.assert_failed_login(&page)?;

    Ok(())
}

#[test]
#[serial]
fn int_should_login_and_cancel_consent() -> TRes<()> {
    let minchir = test_util::setup()?;

    let page = minchir.start_login_flow()?;
    let page = minchir.submit_login_form(
        &page,
        Some(String::from("janedoe")),
        Some(String::from("123")),
        Some(String::from("45")),
        true,
        true,
    )?;
    let page = minchir.submit_consent_form(&page, true, false)?;
    minchir.assert_cancelled_consent(&page)?;

    Ok(())
}

#[test]
#[serial]
fn int_should_login_and_consent_and_cancel_logout() -> TRes<()> {
    let minchir = test_util::setup()?;

    let page = minchir.start_login_flow()?;
    let page = minchir.submit_login_form(
        &page,
        Some(String::from("janedoe")),
        Some(String::from("12345")),
        Some(String::from("")),
        true,
        true,
    )?;
    let page = minchir.submit_consent_form(&page, true, true)?;
    minchir.assert_logged_in_and_redirected(&page)?;

    let page = minchir.start_logout_flow()?;
    let page = minchir.submit_logout_form(&page, false)?;

    // TODO LogoutHandler doesn't currently redirect anywhere useful
    if *page.status() != StatusCode::OK {
        panic!("int_should have gotten 200 - OK!");
    }
    assert_eq!(page.url().to_string(), "https://www.rust-lang.org/");

    Ok(())
}

#[test]
#[serial]
fn int_should_login_and_consent_and_remember_login() -> TRes<()> {
    let minchir = test_util::setup()?;

    let page = minchir.start_login_flow()?;
    let page = minchir.submit_login_form(
        &page,
        Some(String::from("janedoe")),
        Some(String::from("12345")),
        Some(String::from("")),
        true,
        true,
    )?;
    let page = minchir.submit_consent_form(&page, true, true)?;
    minchir.assert_logged_in_and_redirected(&page)?;

    let page = minchir.start_login_flow()?;
    minchir.assert_logged_in_and_redirected(&page)?;

    Ok(())
}

#[test]
#[serial]
fn int_should_login_and_consent_and_logout_and_login_and_remember_consent() -> TRes<()> {
    let minchir = test_util::setup()?;

    int_should_login_and_consent_and_logout()?;

    let page = minchir.start_login_flow()?;
    let page = minchir.submit_login_form(
        &page,
        Some(String::from("janedoe")),
        Some(String::from("12345")),
        Some(String::from("")),
        true,
        true,
    )?;
    minchir.assert_logged_in_and_redirected(&page)?;

    Ok(())
}

fn int_should_fail_with_bad_challenge(
    challenge_type: &str,
    challenge_value: Option<&str>,
) -> TRes<()> {
    let challenge_name = format!("{}_challenge", challenge_type);

    let minchir = test_util::setup()?;

    let conf = minchir.conf();
    let browser = minchir.browser();

    let url = format!("{}/{}", conf.minchir_url, challenge_type);
    let mut query = Vec::new();
    if challenge_value.is_some() {
        query = vec![(challenge_name.as_str(), "not_a_real_challenge_id")];
    }

    let query = if !query.is_empty() {
        Some(&query)
    } else {
        None
    };

    let page = browser.navigate_to(&url, query)?;

    if *page.status() != StatusCode::BAD_REQUEST {
        panic!("int_should have gotten 400 - Bad Request!");
    }

    Ok(())
}

#[test]
#[serial]
fn int_should_fail_with_random_login_challenge() -> TRes<()> {
    int_should_fail_with_bad_challenge("login", Some("illegal_login_challenge"))?;

    Ok(())
}

#[test]
#[serial]
fn int_should_fail_with_random_consent_challenge() -> TRes<()> {
    int_should_fail_with_bad_challenge("consent", Some("illegal_consent_challenge"))?;

    Ok(())
}

#[test]
#[serial]
fn int_should_fail_with_random_logout_challenge() -> TRes<()> {
    int_should_fail_with_bad_challenge("logout", Some("illegal_logout_challenge"))?;

    Ok(())
}

#[test]
#[serial]
fn int_should_fail_when_missing_mandatory_login_challenge() -> TRes<()> {
    int_should_fail_with_bad_challenge("login", None)?;

    Ok(())
}

#[test]
#[serial]
fn int_should_fail_when_using_bad_csrf_token() -> TRes<()> {
    let minchir = test_util::setup()?;

    let page = minchir.start_login_flow()?;

    let form = page.form(0)?;
    form.input(InputType::Hidden, "csrf_token")?
        .as_ref()
        .borrow_mut()
        .set_value(Some(String::from("11111111111")));
    let page = minchir.browser().submit_form(&form, "login")?;

    if *page.status() != StatusCode::BAD_REQUEST {
        panic!("int_should have gotten 400 - Bad Request!");
    }

    Ok(())
}
