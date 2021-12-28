use std::env;

#[derive(Default, Debug, Clone)]
pub struct Config {
    pub queue_url: String,
    pub ecs_cluster: String,
    pub ecs_service: String,
    pub metric_name: String,
    pub metric_namespace: String,
    pub metric_dimension_name: String,
    pub metric_dimension_value: String,
    pub maximum_allowed_latency: u32,
    pub average_processing_time: u32,
    pub queue_workers_upper_bound: u32,
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
            metric_name: get_env_var("METRIC_NAME", Some("BacklogPerECSTask".to_owned())),
            metric_namespace: get_env_var(
                "METRIC_NAMESPACE",
                Some("ECS-SQS-Autoscaling".to_owned()),
            ),
            metric_dimension_name: get_env_var(
                "METRIC_DIMENSION_NAME",
                Some("SQS-Queue".to_owned()),
            ),
            metric_dimension_value: get_env_var(
                "METRIC_DIMENSION_VALUE",
                Some("spread-prod-worker".to_owned()),
            ),
            maximum_allowed_latency: get_env_var("MAXIMUM_ALLOWED_LATENCY", Some("10".to_owned()))
                .parse::<u32>()
                .expect("Could not parse latency value"),
            average_processing_time: get_env_var("AVERAGE_PROCESSING_TIME", Some("2".to_owned()))
                .parse::<u32>()
                .expect("Could not parse average processing time value"),
            queue_workers_upper_bound: get_env_var(
                "QUEUE_WORKERS_UPPER_BOUND",
                Some("10".to_owned()),
            )
            .parse::<u32>()
            .expect("Could not parse queue workers upper bound value"),
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
