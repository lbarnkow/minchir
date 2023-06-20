use std::net::SocketAddr;

use clap::Parser;
use minchir::{cli::Args, error::MinchirError};
use tokio::signal;
use tracing_subscriber::{prelude::__tracing_subscriber_SubscriberExt, util::SubscriberInitExt};

#[tokio::main]
async fn main() -> Result<(), MinchirError> {
    tracing_subscriber::registry()
        .with(tracing_subscriber::EnvFilter::new(
            std::env::var("RUST_LOG").unwrap_or_else(|_| "minchir=debug,tower_http=debug".into()),
        ))
        .with(tracing_subscriber::fmt::layer())
        .init();

    let args = Args::parse();

    let app = minchir::minchir(&args);

    if let Err(error) = app {
        kill_process_with_error_message(&error, 1);
        return Err(error);
    }
    let app = app.unwrap();

    let addr = format!("0.0.0.0:{}", app.settings.config.server().port())
        .parse::<SocketAddr>()
        .unwrap();
    tracing::debug!("Listening on {addr}.");

    let srv = axum::Server::try_bind(&addr);
    if let Err(error) = srv {
        kill_process_with_error_message(error.into_cause().unwrap().as_ref(), 1);
        return Ok(());
    }

    srv.unwrap()
        .serve(app.router.into_make_service())
        .with_graceful_shutdown(shutdown_signal())
        .await
        .unwrap();

    tracing::info!("Shutting down!");

    Ok(())
}

async fn shutdown_signal() {
    match signal::ctrl_c().await {
        Ok(()) => {}
        Err(error) => kill_process_with_error_message(&error, 1),
    }
}

fn kill_process_with_error_message(error: &dyn std::error::Error, exit_code: i32) {
    let mut error = Some(error);

    while error.is_some() {
        let e = error.unwrap();
        tracing::error!("{e}");
        error = e.source();
    }

    std::process::exit(exit_code);
}
