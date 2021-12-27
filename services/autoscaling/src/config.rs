use std::env;

#[derive(Default, Debug, Clone)]
pub struct Config {
    pub queue_url: String,
    pub ecs_cluster: String,
    pub ecs_service: String,
    pub queue_name: String,
    pub metric_name: String,
    pub metric_namespace: String,
}

impl Config {
    pub fn load() -> Self {
        Config {
            queue_url: get_env_var(
                "WORKERS_QUEUE_URL",
                Some(
                    "https://sqs.us-east-2.amazonaws.com/816997647674/spread-prod-worker"
                        .to_owned(),
                ),
            ),
            ecs_cluster: get_env_var("ECS_CLUSTER", Some("spread-ecs-prod".to_owned())),
            ecs_service: get_env_var("ECS_SERVICE", Some("spread-worker".to_owned())),
            queue_name: get_env_var("QUEUE_NAME", Some("spread-prod-worker".to_owned())),
            metric_name: get_env_var("METRIC_NAME", Some("BacklogPerECSTask".to_owned())),
            metric_namespace: get_env_var(
                "METRIC_NAMESPACE",
                Some("ECS-SQS-Autoscaling".to_owned()),
            ),
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
