-- :name upsert-bayes-factor-analysis :! :n
-- :doc Upsert a log file
INSERT INTO bayes_factor_analysis(
id,
user_id,
log_file_url,
locations_file_url,
number_of_locations,
burn_in,
readable_name,
created_on
)
VALUES (
:id,
:user-id,
:log-file-url,
:locations-file-url,
:number-of-locations,
:burn-in,
:readable-name,
:created-on
)
ON DUPLICATE KEY UPDATE
user_id = user_id,
created_on = created_on,
log_file_url = IF(:log-file-url IS NOT NULL, :log-file-url, log_file_url),
locations_file_url = IF(:locations-file-url IS NOT NULL, :locations-file-url, locations_file_url),
number_of_locations = IF(:number-of-locations IS NOT NULL, :number-of-locations, number_of_locations),
burn_in = IF(:burn-in IS NOT NULL, :burn-in, burn_in),
readable_name = :readable-name

-- :name update-bayes-factor-analysis :! :n
-- :doc Updates a continuous tree

UPDATE bayes_factor_analysis
SET
readable_name = IF(:readable-name IS NOT NULL, :readable-name, readable_name),
burn_in = IF(:burn-in IS NOT NULL, :burn-in, burn_in),
locations_file_url = IF(:locations-file-url IS NOT NULL, :locations-file-url, locations_file_url),
number_of_locations = IF(:number-of-locations IS NOT NULL, :number-of-locations, number_of_locations),
output_file_url = IF(:output-file-url IS NOT NULL, :output-file-url, output_file_url)
WHERE id = :id

-- :name delete-bayes-factor-analysis :! :n
-- :doc Delete a tree by id

DELETE
FROM bayes_factor_analysis
WHERE id = :id

-- :name get-bayes-factor-analysis :? :1
-- :doc Get entity by id

SELECT
id,
user_id,
created_on,
log_file_url,
locations_file_url,
burn_in,
status,
progress,
output_file_url,
readable_name,
status,
progress
FROM bayes_factor_analysis
JOIN bayes_factor_analysis_status ON bayes_factor_analysis_status.bayes_factor_analysis_id = bayes_factor_analysis.id
WHERE id = :id

-- :name insert-bayes-factors :! :n
-- :doc Insert bayes factors

INSERT INTO bayes_factors (bayes_factor_analysis_id, bayes_factors)
VALUES (:bayes-factor-analysis-id, :bayes-factors)
ON DUPLICATE KEY UPDATE
bayes_factor_analysis_id = :bayes-factor-analysis-id,
bayes_factors = :bayes-factors

-- :name get-bayes-factors :? :1
-- :doc Get bayes factor JSON document by it's analysis id

SELECT
bayes_factor_analysis_id,
bayes_factors
FROM bayes_factors
WHERE bayes_factor_analysis_id = :bayes-factor-analysis-id

-- :name upsert-status :! :n
-- :doc Upsert a continuous tree status

INSERT INTO bayes_factor_analysis_status(
bayes_factor_analysis_id,
status,
progress
)
VALUES (
:bayes-factor-analysis-id,
:status,
:progress
)
ON DUPLICATE KEY UPDATE
status = IF(:status IS NOT NULL, :status, status),
progress = IF(:progress IS NOT NULL, :progress, progress)

-- :name get-status :? :1
-- :doc Get analysis status by id

SELECT
bayes_factor_analysis_id,
status,
progress
FROM bayes_factor_analysis_status
WHERE :bayes-factor-analysis-id = bayes_factor_analysis_id
