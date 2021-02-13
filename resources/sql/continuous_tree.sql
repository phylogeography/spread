-- :name upsert-tree :! :n
-- :doc Upsert a continuous tree

INSERT INTO continuous_tree(
id,
user_id,
tree_file_url,
status,
readable_name
)
VALUES (
:id,
:user-id,
:tree-file-url,
:status,
:readable-name
)
ON DUPLICATE KEY UPDATE
user_id = user_id,
tree_file_url = IF(:tree-file-url IS NOT NULL, :tree-file-url, tree_file_url),
status = :status,
readable_name = :readable-name

-- :name update-tree :! :n
-- :doc Updates a continuous tree

UPDATE continuous_tree
SET
status = :status,
readable_name = IF(:readable-name IS NOT NULL, :readable-name, readable_name),
x_coordinate_attribute_name = IF(:x-coordinate-attribute-name IS NOT NULL, :x-coordinate-attribute-name, x_coordinate_attribute_name),
y_coordinate_attribute_name = IF(:y-coordinate-attribute-name IS NOT NULL, :y-coordinate-attribute-name, y_coordinate_attribute_name),
hpd_level = IF(:hpd-level IS NOT NULL, :hpd-level, hpd_level),
has_external_annotations = IF(:has-external-annotations IS NOT NULL, :has-external-annotations, has_external_annotations),
timescale_multiplier = IF(:timescale-multiplier IS NOT NULL, :timescale-multiplier, timescale_multiplier),
most_recent_sampling_date = IF(:most-recent-sampling-date IS NOT NULL, :most-recent-sampling-date, most_recent_sampling_date),
output_file_url = IF(:output-file-url IS NOT NULL, :output-file-url, output_file_url)
WHERE id = :id

-- :name delete-tree :! :n
-- :doc Delete a tree by id

DELETE
FROM continuous_tree
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

SELECT attribute_name
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

-- :name get-status :? :1
-- :doc Get analysis status by id

SELECT
id,
status
FROM continuous_tree
WHERE :id = id
