-- :name upsert-analysis :! :n
-- :doc Upsert an analysis

INSERT INTO analysis(
id,
of_type,
user_id,
readable_name,
created_on,
status,
progress,
viewer_url_params
)
VALUES (
:id,
:of-type,
:user-id,
:readable-name,
:created-on,
:status,
:progress,
:viewer-url-params
)
ON DUPLICATE KEY UPDATE
of_type           = IF(:of-type IS NOT NULL, :of-type, of_type),
user_id           = IF(:user-id IS NOT NULL, :user-id, user_id),
readable_name     = IF(:readable-name IS NOT NULL, :readable-name, readable_name),
created_on        = IF(:created-on IS NOT NULL, :created-on, created_on),
status            = IF(:status IS NOT NULL, :status, status),
progress          = IF(:progress IS NOT NULL, :progress, progress),
viewer_url_params = IF(:viewer-url-params IS NOT NULL, :viewer-url-params, viewer_url_params)

-- :name delete-analysis :! :n
-- :doc Delete an analysis by id

DELETE
FROM analysis
WHERE id = :id

-- :name delete-all-user-analysis :! :n
-- :doc Delete all analysis for that user

DELETE
FROM analysis
WHERE user_id = :user-id

-- :name get-analysis :? :1
-- :doc Return a analysis by id

SELECT
id,
of_type,
user_id,
readable_name,
created_on,
status,
progress,
is_new,
viewer_url_params
FROM analysis
WHERE id = :id

-- :name get-user-analysis :? :*
-- :doc Return a list of all user analysis

SELECT
id,
created_on,
of_type,
readable_name,
status,
progress,
is_new,
viewer_url_params
FROM analysis
WHERE user_id = :user-id AND of_type <> "TIME_SLICER"

-- :name touch-analysis :! :n
-- :doc Mark analysis as seen by the user

UPDATE analysis
SET
is_new = FALSE
WHERE id = :id
