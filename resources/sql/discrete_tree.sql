-- :name upsert-tree :! :n
-- :doc Upsert a discrete tree

INSERT INTO discrete_tree(
id,
user_id,
tree_file_url,
locations_file_url,
status,
readable_name
)
VALUES (
:id,
:user-id,
:tree-file-url,
:locations-file-url,
:status,
:readable-name
)
ON DUPLICATE KEY UPDATE
user_id = user_id,
tree_file_url = IF(:tree-file-url IS NOT NULL, :tree-file-url, tree_file_url),
locations_file_url = IF(:locations-file-url IS NOT NULL, :locations-file-url, locations_file_url),
status = :status,
readable_name = :readable-name
