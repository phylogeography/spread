-- :name upsert-bayes-factor-analysis :! :n
-- :doc Upsert a log file
INSERT INTO bayes_factor_analysis(
id,
user_id,
log_file_url,
locations_file_url,
number_of_locations,
burn_in,
status,
readable_name
)
VALUES (
:id,
:user-id,
:log-file-url,
:locations-file-url,
:number-of-locations,
:burn-in,
:status,
:readable-name
)
ON DUPLICATE KEY UPDATE
user_id = user_id,
log_file_url = IF(:log-file-url IS NOT NULL, :log-file-url, log_file_url),
locations_file_url = IF(:locations-file-url IS NOT NULL, :locations-file-url, locations_file_url),
number_of_locations = IF(:number-of-locations IS NOT NULL, :number-of-locations, number_of_locations),
burn_in = IF(:burn-in IS NOT NULL, :burn-in, burn_in),
status = :status,
readable_name = :readable-name

-- :name update-bayes-factor-analysis :! :n
-- :doc Updates a continuous tree

UPDATE bayes_factor_analysis
SET
status = :status,
readable_name = IF(:readable-name IS NOT NULL, :readable-name, readable_name),
burn_in = IF(:burn-in IS NOT NULL, :burn-in, burn_in),
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
log_file_url,
locations_file_url,
burn_in,
status,
output_file_url,
readable_name
FROM bayes_factor_analysis
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
