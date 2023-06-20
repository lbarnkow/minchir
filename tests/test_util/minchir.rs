use super::{hydra, TRes, TestConfig};
use no_browser::browser::Browser;
use no_browser::page::Page;
use no_browser::InputType;
use reqwest::Method;

pub struct MinchirClient {
    conf: TestConfig,
    browser: Browser,
}

impl MinchirClient {
    pub fn build(conf: TestConfig) -> TRes<Self> {
        Ok(Self {
            conf,
            browser: Browser::builder().skip_tls_verify(true).finish()?,
        })
    }

    pub fn conf(&self) -> &TestConfig {
        &self.conf
    }

    pub fn browser(&self) -> &Browser {
        &self.browser
    }

    pub fn start_login_flow(&self) -> TRes<Page> {
        let (url, query) = hydra::auth_code_flow_url(self.conf);
        let page = self.browser.navigate_to(&url, Some(&query))?;

        Ok(page)
    }

    pub fn start_logout_flow(&self) -> TRes<Page> {
        let url = hydra::logout_url(self.conf);
        Ok(self.browser.navigate_to(&url, None)?)
    }

    pub fn submit_login_form(
        &self,
        page: &Page,
        username: Option<String>,
        password: Option<String>,
        totp: Option<String>,
        remember_me: bool,
        submit: bool,
    ) -> TRes<Page> {
        assert_page_is_login_form(&page, self.conf)?;

        let form = page.form(0)?;

        if username.is_some() {
            form.input(InputType::Text, "username")?
                .as_ref()
                .borrow_mut()
                .set_value(username);
        }
        if password.is_some() {
            form.input(InputType::Password, "password")?
                .as_ref()
                .borrow_mut()
                .set_value(password);
        }
        if totp.is_some() {
            form.input(InputType::Password, "totp")?
                .as_ref()
                .borrow_mut()
                .set_value(totp);
        }
        if remember_me {
            form.input(InputType::Checkbox, "remember_me")?
                .as_ref()
                .borrow_mut()
                .set_attr("checked", Some("checked".to_owned()));
        }

        let btn = if submit { "login" } else { "cancel" };
        Ok(self.browser.submit_form(&form, btn)?)
    }

    pub fn submit_consent_form(
        &self,
        page: &Page,
        remember_consent: bool,
        submit: bool,
    ) -> TRes<Page> {
        assert_page_is_consent_form(&page, self.conf)?;

        let form = page.form(0)?;
        if remember_consent {
            form.input(InputType::Checkbox, "remember_consent")?
                .as_ref()
                .borrow_mut()
                .set_attr("checked", Some("checked".to_owned()));
        }

        let btn = if submit { "consent" } else { "cancel" };
        Ok(self.browser.submit_form(&form, btn)?)
    }

    pub fn submit_logout_form(&self, page: &Page, submit: bool) -> TRes<Page> {
        assert_page_is_logout_form(&page, self.conf)?;

        let form = page.form(0)?;
        let btn = if submit { "logout" } else { "cancel" };
        Ok(self.browser.submit_form(&form, btn)?)
    }

    pub fn assert_logged_in_and_redirected(&self, page: &Page) -> TRes<()> {
        assert_prop_starts_with(
            &page.url().to_string(),
            self.conf.login_callback,
            "Page URL",
        )?;

        let query = page.url().query().unwrap();
        assert_prop_contains(
            query,
            &format!("scope={}", self.conf.oidc_scope),
            "Page URL query",
        )?;
        assert_prop_contains(
            query,
            &format!("state={}", self.conf.oidc_state),
            "Page URL query",
        )?;
        assert_prop_contains(query, "code=", "Page URL query")?;
        Ok(())
    }

    pub fn assert_logged_out_and_redirected(&self, page: &Page) -> TRes<()> {
        assert_prop_starts_with(
            &page.url().to_string(),
            self.conf.logout_callback,
            "Page URL",
        )?;

        let query = page.url().query();
        assert_prop_is_empty(query, "Page URL query")?;
        Ok(())
    }

    pub fn assert_cancelled_login(&self, page: &Page) -> TRes<()> {
        assert_prop_starts_with(
            &page.url().to_string(),
            self.conf.login_callback,
            "Page URL",
        )?;

        let query = page.url().query().unwrap();
        assert_prop_contains(query, "error=access_denied", "Page URL query")?;
        assert_prop_contains(
            query,
            &format!("state={}", self.conf.oidc_state),
            "Page URL query",
        )?;
        Ok(())
    }

    pub fn assert_cancelled_consent(&self, page: &Page) -> TRes<()> {
        self.assert_cancelled_login(page)
    }

    pub fn assert_failed_login(&self, page: &Page) -> TRes<()> {
        assert_page_is_login_form(page, self.conf)?;

        assert_prop_is_equal_to(page.status().as_str(), "401", "HTTP Status code")?;
        assert_prop_contains(
            page.text(),
            "Login failed; your credentials were incorrect.",
            "Page text",
        )?;

        Ok(())
    }
}

fn assert_page_is_login_form(page: &Page, conf: TestConfig) -> TRes<()> {
    assert_prop_starts_with(
        &page.url().to_string(),
        &format!("{}/login", conf.minchir_url),
        "Page URL",
    )?;

    assert_prop_is_equal_to(
        &page.select_first("head > title")?.inner_html(),
        "SSO - Login",
        "Page title",
    )?;

    if page.method() == Method::GET {
        let query_challenge = page.query("login_challenge")?;
        let form = page.form(0)?;
        let form_challenge = form
            .input(InputType::Hidden, "login_challenge")?
            .borrow()
            .value()
            .unwrap_or("").to_owned();

        assert_prop_is_equal_to(
            &query_challenge,
            &form_challenge,
            "Query param login_challenge",
        )?;
    }

    Ok(())
}

fn assert_page_is_consent_form(page: &Page, conf: TestConfig) -> TRes<()> {
    assert_prop_starts_with(
        &page.url().to_string(),
        &format!("{}/consent", conf.minchir_url),
        "Page URL",
    )?;

    assert_prop_is_equal_to(
        &page.select_first("head > title")?.inner_html(),
        "SSO - Consent",
        "Page title",
    )?;

    if page.method() == Method::GET {
        let query_challenge = page.query("consent_challenge")?;
        let form = page.form(0)?;
        let form_challenge = form
            .input(InputType::Hidden, "consent_challenge")?
            .borrow()
            .value()
            .unwrap_or("").to_owned();

        assert_prop_is_equal_to(
            &query_challenge,
            &form_challenge,
            "Query param consent_challenge",
        )?;
    }

    Ok(())
}

fn assert_page_is_logout_form(page: &Page, conf: TestConfig) -> TRes<()> {
    assert_prop_starts_with(
        &page.url().to_string(),
        &format!("{}/logout", conf.minchir_url),
        "Page URL",
    )?;

    assert_prop_is_equal_to(
        &page.select_first("head > title")?.inner_html(),
        "SSO - Logout",
        "Page title",
    )?;

    if page.method() == Method::GET {
        let query_challenge = page.query("logout_challenge")?;
        let form = page.form(0)?;
        let form_challenge = form
            .input(InputType::Hidden, "logout_challenge")?
            .borrow()
            .value()
            .unwrap_or("").to_owned();

        assert_prop_is_equal_to(
            &query_challenge,
            &form_challenge,
            "Query param logout_challenge",
        )?;
    }

    Ok(())
}

fn assert_prop_starts_with(left: &str, right: &str, prop: &str) -> TRes<()> {
    if !left.starts_with(right) {
        return Err(super::MinchirTestError::BinaryPropComparisonError {
            prop: String::from(prop),
            reason: String::from("does not start with"),
            lhs: String::from(left),
            rhs: String::from(right),
        });
    }

    Ok(())
}

fn assert_prop_contains(left: &str, right: &str, prop: &str) -> TRes<()> {
    if !left.contains(right) {
        return Err(super::MinchirTestError::BinaryPropComparisonError {
            prop: String::from(prop),
            reason: String::from("does not contain"),
            lhs: String::from(left),
            rhs: String::from(right),
        });
    }

    Ok(())
}

fn assert_prop_is_equal_to(left: &str, right: &str, prop: &str) -> TRes<()> {
    if !left.eq(right) {
        return Err(super::MinchirTestError::BinaryPropComparisonError {
            prop: String::from(prop),
            reason: String::from("is not equal to"),
            lhs: String::from(left),
            rhs: String::from(right),
        });
    }

    Ok(())
}

fn assert_prop_is_empty(left: Option<&str>, prop: &str) -> TRes<()> {
    if left.is_some() && !left.unwrap().is_empty() {
        return Err(super::MinchirTestError::UnaryPropComparisonError {
            prop: String::from(prop),
            reason: String::from("is not empty"),
            lhs: String::from(left.unwrap()),
        });
    }

    Ok(())
}
