-- :name upsert-analysis :! :n
-- :doc Upsert an analysis

INSERT INTO analysis(
id,
of_type,
user_id,
readable_name,
created_on,
status,
progress
)
VALUES (
:id,
:of-type
:user-id,
:readable-name,
:created-on,
:status,
:progress
)
ON DUPLICATE KEY UPDATE
user_id = user_id,
readable_name = IF(:readable-name IS NOT NULL, :readable-name, readable_name),
created_on = created_on,
status = IF(:status IS NOT NULL, :status, status),
of_type = of_type,
progress = IF(:progress IS NOT NULL, :progress, progress)

-- :name delete-analysis :! :n
-- :doc Delete an analysis by id

DELETE
FROM analysis
WHERE id = :id

-- -- :name upsert-status :! :n
-- -- :doc Upsert analysis status

-- INSERT INTO analysis(
-- id,
-- status,
-- progress
-- )
-- VALUES (
-- :id,
-- :status,
-- :progress
-- )
-- ON DUPLICATE KEY UPDATE
-- status = IF(:status IS NOT NULL, :status, status),
-- progress = IF(:progress IS NOT NULL, :progress, progress)
