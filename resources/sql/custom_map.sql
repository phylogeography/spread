-- :name upsert-custom-map :! :n
-- :doc Upsert a custom map

INSERT INTO custom_map(
analysis_id,
file_name,
file_url
)
VALUES (
:analysis-id,
:file-name,
:file-url
)
ON DUPLICATE KEY UPDATE
file_name           = IF(:file-name IS NOT NULL, :file-name, file_name),
file_url           = IF(:file-url IS NOT NULL, :file-url, file_url)

-- :name delete-custom-map :! :n
-- :doc Delete an custom map by analysis-id

DELETE
FROM custom_map
WHERE analysis_id = :analysis-id

-- :name get-custom-map :? :1
-- :doc Return a custom map by id

SELECT
analysis_id,
file_name,
file_url
FROM custom_map
WHERE analysis_id = :analysis-id
