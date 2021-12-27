mod config;

use aws_config::meta::region::RegionProviderChain;
use aws_sdk_cloudwatch::{
    model::{Dimension, MetricDatum},
    Client as CloudwatchClient, Error as CloudwatchError,
};
use aws_sdk_ecs::Client as EcsClient;
use aws_sdk_sqs::{model::QueueAttributeName, Client as SqsClient, Error as SqsError};
use config::Config;
use log::{debug, info};
use std::env;

#[tokio::main]
async fn main() -> Result<(), anyhow::Error> {
    init_logging();

    let Config {
        queue_url,
        ecs_cluster,
        ecs_service,
        metric_namespace,
        metric_name,
        queue_name,
        ..
    } = Config::load();

    let aws_config = aws_config::load_from_env().await;
    let cloudwatch_client = CloudwatchClient::new(&aws_config);
    let sqs_client = SqsClient::new(&aws_config);
    let ecs_client = EcsClient::new(&aws_config);

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

    // aws ecs list-tasks --cluster spread-ecs-prod --service-name spread-worker

    let tasks_count = ecs_client
        .list_tasks()
        .cluster(ecs_cluster)
        .service_name(ecs_service)
        .send()
        .await?
        .task_arns
        .expect("Could not list ecs service tasks")
        .len() as u32;

    info!("tasks count {}", tasks_count);

    //$( ( ($ApproximateNumberOfMessages / $NUMBER_TASKS) + ($ApproximateNumberOfMessages % $NUMBER_TASKS > 0) ) )

    let backlog_per_worker = messages_in_flight / tasks_count;

    info!("backlog_per_worker {}", backlog_per_worker);

    // aws cloudwatch put-metric-data --namespace ECS-SQS-Autoscaling --metric-name BacklogPerECSTask --unit None --value 1 --dimensions SQS-Queue=spread-prod-worker

    let dimension = Dimension::builder()
        .set_name(Some("SQS-Queue".to_owned()))
        .set_value(Some(queue_name))
        .build();
    let datum = MetricDatum::builder()
        .set_metric_name(Some(metric_name))
        .set_unit(None)
        .set_value(Some(backlog_per_worker as f64))
        .set_dimensions(Some(vec![dimension]))
        .build();
    let data = vec![datum];

    cloudwatch_client
        .put_metric_data()
        .set_metric_data(Some(data))
        .set_namespace(Some(metric_namespace))
        .send()
        .await
        .expect("Could not send metric data");

    Ok(())
}

fn init_logging() {
    if env::var(env_logger::DEFAULT_FILTER_ENV).is_err() {
        env::set_var(env_logger::DEFAULT_FILTER_ENV, "info");
    }
    env_logger::init();
}
