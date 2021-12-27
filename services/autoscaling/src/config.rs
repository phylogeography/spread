use std::env;

#[derive(Default, Debug, Clone)]
pub struct Config {
    pub queue_url: String,
}

impl Config {
    pub fn load() -> Self {
        Config {
            queue_url: get_env_var("WORKERS_QUEUE_URL", None),
        }
    }
}

fn get_env_var(var: &str, default: Option<String>) -> String {
    match env::var(var) {
        Ok(v) => v,
        Err(_) => match default {
            None => panic!("Missing ENV variable: {} not defined in environment", var),
            Some(d) => d,
        },
    }
}
