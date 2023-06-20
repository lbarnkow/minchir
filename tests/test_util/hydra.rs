use super::{TRes, TestConfig};

const ORY_AUTH_ENDPOINT: &str = "/oauth2/auth";
const ORY_LOGOUT_ENPOINT: &str = "/oauth2/sessions/logout";

const ORY_ADMIN_CONSENT_SESSIONS_ENDPOINT: &str = "/oauth2/auth/sessions/consent";

pub fn clean_consent_sessions(conf: TestConfig) -> TRes<()> {
    let client = reqwest::blocking::ClientBuilder::new()
        .danger_accept_invalid_certs(true)
        .build()?;

    let url = format!(
        "{}{}",
        conf.ory_admin_url, ORY_ADMIN_CONSENT_SESSIONS_ENDPOINT
    );

    let response = client
        .delete(url)
        .query(&[("subject", "janedoe")])
        .query(&[("all", "true")])
        .send()?;

    assert!(response.status().is_success());

    Ok(())
}

pub fn auth_code_flow_url(conf: TestConfig) -> (String, Vec<(&'static str, &'static str)>) {
    let url = format!("{}{}", conf.ory_url, ORY_AUTH_ENDPOINT);

    let mut query = Vec::new();
    query.push(("client_id", conf.client_id));
    query.push(("response_type", conf.oidc_response_type));
    query.push(("scope", conf.oidc_scope));
    query.push(("redirect_uri", conf.login_callback));
    query.push(("state", conf.oidc_state));

    (url, query)
}

pub fn logout_url(conf: TestConfig) -> String {
    format!("{}{}", conf.ory_url, ORY_LOGOUT_ENPOINT)
}
