use crate::config::Config;
use aws_sdk_cloudwatch::DateTime;
use aws_sdk_cloudwatch::{
    model::{Dimension, MetricDatum, Statistic},
    Client as CloudwatchClient,
};
use log::{debug, info};

pub async fn run(config: Config) -> Result<(), anyhow::Error> {
    let Config {
        metric_dimension_name,
        metric_dimension_value,
        // ecs_cluster,
        // ecs_service,
        metric_namespace,
        metric_name,
        ..
    } = config;

    let aws_config = aws_config::load_from_env().await;
    let cloudwatch_client = CloudwatchClient::new(&aws_config);

    // aws cloudwatch get-metric-statistics --namespace $CW_NAMESPACE --dimensions Name=$CW_DIMENSION_NAME,Value=$CW_DIMENSION_VALUE --metric-name $CW_METRIC  --start-time "$(date -u --date='5 minutes ago')" --end-time "$(date -u)"  --period 60 --statistics Average

    let dimension = Dimension::builder()
        .set_name(Some(metric_dimension_name))
        .set_value(Some(metric_dimension_value))
        .build();

    let start_time = DateTime::from(
        std::time::SystemTime::now()
            .checked_sub(std::time::Duration::from_secs(5 * 60))
            .unwrap(),
    );

    let end_time = DateTime::from(std::time::SystemTime::now());

    let stats = cloudwatch_client
        .get_metric_statistics()
        .set_namespace(Some(metric_namespace))
        .set_dimensions(Some(vec![dimension]))
        .set_metric_name(Some(metric_name))
        .set_start_time(Some(start_time))
        .set_end_time(Some(end_time))
        .set_period(Some(60))
        .set_statistics(Some(vec![Statistic::Average]))
        .send()
        .await
        .expect("Could not get metric statistics")
        .datapoints
        .expect("Could not read datapoints");

    info!("worker backlog statistics {:?}", stats);

    Ok(())
}
