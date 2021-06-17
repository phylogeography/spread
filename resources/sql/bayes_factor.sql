-- :name upsert-bayes-factor-analysis :! :n
-- :doc Upsert a log file
INSERT INTO bayes_factor_analysis(
id,
log_file_url,
locations_file_url,
number_of_locations,
burn_in,
output_file_url
)
VALUES (
:id,
:log-file-url,
:locations-file-url,
:number-of-locations,
:burn-in,
:output-file-url
)
ON DUPLICATE KEY UPDATE
log_file_url =        IF(:log-file-url IS NOT NULL, :log-file-url, log_file_url),
locations_file_url =  IF(:locations-file-url IS NOT NULL, :locations-file-url, locations_file_url),
number_of_locations = IF(:number-of-locations IS NOT NULL, :number-of-locations, number_of_locations),
burn_in =             IF(:burn-in IS NOT NULL, :burn-in, burn_in),
output_file_url =     IF(:output-file-url IS NOT NULL, :output-file-url, output_file_url)

-- :name get-bayes-factor-analysis :? :1
-- :doc Get entity by id

SELECT
analysis.id,
user_id,
created_on,
log_file_url,
locations_file_url,
number_of_locations,
burn_in,
status,
progress,
output_file_url,
readable_name,
status,
progress
FROM bayes_factor_analysis
JOIN analysis ON analysis.id = bayes_factor_analysis.id
WHERE analysis.id = :id

-- :name insert-bayes-factors :! :n
-- :doc Insert bayes factors

INSERT INTO bayes_factors (id, bayes_factors)
VALUES (:id, :bayes-factors)
ON DUPLICATE KEY UPDATE
id = :id,
bayes_factors = :bayes-factors

-- :name get-bayes-factors :? :1
-- :doc Get bayes factor JSON document by it's analysis id

SELECT
id,
bayes_factors
FROM bayes_factors
WHERE id = :id
