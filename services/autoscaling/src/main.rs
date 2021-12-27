mod backlog;
mod config;
mod scaling;

use config::Config;
use log::{debug, info};
use std::env;
use std::time::Duration;
use tokio::time;

#[tokio::main]
async fn main() -> Result<(), anyhow::Error> {
    init_logging();
    let mut tasks = Vec::with_capacity(2);

    // let config_ptr1 = Arc::clone(&config);
    tasks.push(tokio::spawn(async {
        let mut interval = time::interval(Duration::from_secs(60));
        loop {
            backlog::publish(Config::load())
                .await
                .expect("backlog publish failed unexpectedly");
            interval.tick().await;
        }
    }));

    tasks.push(tokio::spawn(async {
        let mut interval = time::interval(Duration::from_secs(600));
        loop {
            scaling::run(Config::load())
                .await
                .expect("scaling failed unexpectedly");
            interval.tick().await;
        }
    }));

    for t in tasks {
        t.await.expect("Ooops!");
    }

    Ok(())
}

fn init_logging() {
    if env::var(env_logger::DEFAULT_FILTER_ENV).is_err() {
        env::set_var(env_logger::DEFAULT_FILTER_ENV, "info");
    }
    env_logger::init();
}
