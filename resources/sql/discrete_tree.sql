-- :name upsert-tree :! :n
-- :doc Upsert a discrete tree

INSERT INTO discrete_tree(
id,
user_id,
tree_file_url,
locations_file_url,
readable_name,
created_on
)
VALUES (
:id,
:user-id,
:tree-file-url,
:locations-file-url,
:readable-name,
:created-on
)
ON DUPLICATE KEY UPDATE
user_id = user_id,
created_on = created_on,
tree_file_url = IF(:tree-file-url IS NOT NULL, :tree-file-url, tree_file_url),
locations_file_url = IF(:locations-file-url IS NOT NULL, :locations-file-url, locations_file_url),
readable_name = :readable-name

-- :name update-tree :! :n
-- :doc Updates a continuous tree

UPDATE discrete_tree
SET
readable_name = IF(:readable-name IS NOT NULL, :readable-name, readable_name),
location_attribute_name = IF(:location-attribute-name IS NOT NULL, :location-attribute-name, location_attribute_name),
timescale_multiplier = IF(:timescale-multiplier IS NOT NULL, :timescale-multiplier, timescale_multiplier),
most_recent_sampling_date = IF(:most-recent-sampling-date IS NOT NULL, :most-recent-sampling-date, most_recent_sampling_date),
output_file_url = IF(:output-file-url IS NOT NULL, :output-file-url, output_file_url)
WHERE id = :id

-- :name delete-tree :! :n
-- :doc Delete a tree by id

DELETE
FROM discrete_tree
WHERE id = :id

-- :name insert-attribute :! :n
-- :doc Insert an attribute

INSERT INTO discrete_tree_attributes (tree_id, attribute_name)
VALUES (:tree-id, :attribute-name)
ON DUPLICATE KEY UPDATE
tree_id = :tree-id,
attribute_name = :attribute-name

-- :name get-attributes :? :*
-- :doc Get attributes by tree-id

SELECT attribute_name
FROM discrete_tree_attributes
WHERE :tree-id = tree_id

-- :name get-tree :? :1
-- :doc Get entity by id

SELECT
id,
user_id,
tree_file_url,
locations_file_url,
location_attribute_name,
timescale_multiplier,
most_recent_sampling_date,
output_file_url,
readable_name,
status,
progress
FROM discrete_tree
JOIN discrete_tree_status ON discrete_tree_status.tree_id = discrete_tree.id
WHERE :id = id

-- :name upsert-status :! :n
-- :doc Upsert a continuous tree status

INSERT INTO discrete_tree_status(
tree_id,
status,
progress
)
VALUES (
:tree-id,
:status,
:progress
)
ON DUPLICATE KEY UPDATE
status = IF(:status IS NOT NULL, :status, status),
progress = IF(:progress IS NOT NULL, :progress, progress)

-- :name get-status :? :1
-- :doc Get analysis status by id

SELECT
tree_id,
status,
progress
FROM discrete_tree_status
WHERE tree_id = :tree-id
