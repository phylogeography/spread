mod config;

use aws_config::meta::region::RegionProviderChain;
use aws_sdk_cloudwatch::{Client as CloudwatchClient, Error as CloudwatchError};
use aws_sdk_sqs::{model::QueueAttributeName, Client as SqsClient, Error as SqsError};
use config::Config;
use log::{debug, info};
use std::env;

#[tokio::main]
async fn main() -> Result<(), anyhow::Error> {
    init_logging();

    let Config { queue_url } = Config::load();

    let aws_config = aws_config::load_from_env().await;
    let cloudwatch_client = CloudwatchClient::new(&aws_config);

    let sqs_client = SqsClient::new(&aws_config);

    // aws sqs get-queue-attributes --queue-url "https://sqs.us-east-2.amazonaws.com/816997647674/spread-prod-worker" --attribute-names All

    let attributes = sqs_client
        .get_queue_attributes()
        .attribute_names(QueueAttributeName::ApproximateNumberOfMessages)
        .queue_url(&queue_url)
        .send()
        .await
        .expect("Could not get queue attributes")
        .attributes
        .unwrap();

    let messages_in_flight = attributes
        .get(&QueueAttributeName::ApproximateNumberOfMessages)
        .map(|value| value.parse::<u32>().unwrap())
        .expect("Could not read ApproximateNumberOfMessages");

    info!("messages_in_flight {}", messages_in_flight);

    // aws ecs list-tasks --cluster $ECS_CLUSTER --service-name $ECS_SERVICE

    Ok(())
}

fn init_logging() {
    if env::var(env_logger::DEFAULT_FILTER_ENV).is_err() {
        env::set_var(env_logger::DEFAULT_FILTER_ENV, "info");
    }
    env_logger::init();
}
