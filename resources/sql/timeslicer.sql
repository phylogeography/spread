-- :name upsert-timeslicer :! :n
-- :doc Upsert a timeslicer entity

INSERT INTO timeslicer(
id,
user_id,
trees_file_url,
slice_heights_file_url,
status,
readable_name
)
VALUES (
:id,
:user-id,
:trees-file-url,
:slice-heights-file-url,
:status,
:readable-name
)
ON DUPLICATE KEY UPDATE
user_id = user_id,
trees_file_url = IF(:trees-file-url IS NOT NULL, :trees-file-url, trees_file_url),
slice_heights_file_url = IF(:slice-heights-file-url IS NOT NULL, :slice-heights-file-url, slice_heights_file_url),
status = :status,
readable_name = :readable-name
