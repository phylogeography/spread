-- :name upsert-tree :! :n
-- :doc Upsert a continuous tree

INSERT IGNORE INTO continuous_tree(
id,
user_id,
tree_file_url,
x_coordinate_attribute_name,
y_coordinate_attribute_name,
hpd_level,
has_external_annotations,
timescale_multiplier,
most_recent_sampling_date,
status,
output_file_url,
readable_name
)
VALUES (
:id,
:user-id,
:tree-file-url,
:x-coordinate-attribute-name,
:y-coordinate-attribute-name,
:hpd-level,
:has-external-annotations,
:timescale-multiplier,
:most-recent-sampling-date,
:status,
:output-file-url,
:readable-name
)
ON DUPLICATE KEY UPDATE
x_coordinate_attribute_name = :x-coordinate-attribute-name,
y_coordinate_attribute_name = :y-coordinate-attribute-name,
hpd_level = :hpd-level,
has_external_annotations = :has-external-annotations,
timescale_multiplier = :timescale-multiplier,
most_recent_sampling_date = :most-recent-sampling-date,
status = :status,
output_file_url = :output-file-url,
readable_name = :readable-name

-- :name delete-tree :! :n
-- :doc Delete a tree by id
DELETE
FROM continuous_tree
WHERE id = :id

-- :name update-status :! :1
-- :doc Update status of analysis with id

UPDATE continuous_tree
SET
status = :status
WHERE id = :id

-- :name update-output :! :1
-- :doc Update status of analysis with id

UPDATE continuous_tree
SET
output_file_url = :output-file-url
WHERE id = :id

-- :name insert-attribute :! :n
-- :doc Insert an attribute

INSERT INTO continuous_tree_attributes (tree_id, attribute_name)
VALUES (:tree-id, :attribute-name)
ON DUPLICATE KEY UPDATE
tree_id = :tree-id,
attribute_name = :attribute-name

-- :name insert-hpd-level :! :n
-- :doc Insert an hpd level

INSERT INTO continuous_tree_hpd_levels (tree_id, level)
VALUES (:tree-id, :level)
ON DUPLICATE KEY UPDATE
tree_id = :tree-id,
level = :level

-- :name get-attributes :? :*
-- :doc Get attributes by tree-id

SELECT ATTRIBUTE_NAME
FROM continuous_tree_attributes
WHERE :tree-id = tree_id

-- :name get-hpd-levels :? :*
-- :doc Get hpd-levels by tree-id

SELECT level
FROM continuous_tree_hpd_levels
WHERE :tree-id = tree_id

-- :name get-tree :? :1
-- :doc Get entity by id

SELECT
id,
user_id,
tree_file_url,
x_coordinate_attribute_name,
y_coordinate_attribute_name,
hpd_level,
has_external_annotations,
timescale_multiplier,
most_recent_sampling_date,
status,
output_file_url,
readable_name
FROM continuous_tree
WHERE :id = id
