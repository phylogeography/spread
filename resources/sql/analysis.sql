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
:of-type,
:user-id,
:readable-name,
:created-on,
:status,
:progress
)
ON DUPLICATE KEY UPDATE
of_type       = IF(:of-type IS NOT NULL, :of-type, of_type),
user_id       = IF(:user-id IS NOT NULL, :user-id, user_id),
readable_name = IF(:readable-name IS NOT NULL, :readable-name, readable_name),
created_on    = IF(:created-on IS NOT NULL, :created-on, created_on),
status        = IF(:status IS NOT NULL, :status, status),
progress      = IF(:progress IS NOT NULL, :progress, progress)

-- :name delete-analysis :! :n
-- :doc Delete an analysis by id

DELETE
FROM analysis
WHERE id = :id

-- :name get-analysis :? :1
-- :doc Return a analysis by id

SELECT
id,
of_type,
user_id,
readable_name,
created_on,
status,
progress
FROM analysis
WHERE id = :id

-- :name get-user-analysis :? :*
-- :doc Return a list of all user analysis

SELECT
id,
of_type,
readable_name,
status,
progress
FROM analysis
WHERE user_id = :user-id
