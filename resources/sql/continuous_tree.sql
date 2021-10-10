-- :name upsert-tree :! :n
-- :doc Upsert a continuous tree

INSERT INTO continuous_tree(
id,
tree_file_url,
tree_file_name,
x_coordinate_attribute_name,
y_coordinate_attribute_name,
timescale_multiplier,
most_recent_sampling_date,
output_file_url
)
VALUES (
:id,
:tree-file-url,
:tree-file-name,
:x-coordinate-attribute-name,
:y-coordinate-attribute-name,
:timescale-multiplier,
:most-recent-sampling-date,
:output-file-url
)
ON DUPLICATE KEY UPDATE
tree_file_url =               IF(:tree-file-url IS NOT NULL, :tree-file-url, tree_file_url),
tree_file_name =              IF(:tree-file-name IS NOT NULL, :tree-file-name, tree_file_name),
x_coordinate_attribute_name = IF(:x-coordinate-attribute-name IS NOT NULL, :x-coordinate-attribute-name, x_coordinate_attribute_name),
y_coordinate_attribute_name = IF(:y-coordinate-attribute-name IS NOT NULL, :y-coordinate-attribute-name, y_coordinate_attribute_name),
timescale_multiplier =        IF(:timescale-multiplier IS NOT NULL, :timescale-multiplier, timescale_multiplier),
most_recent_sampling_date =   IF(:most-recent-sampling-date IS NOT NULL, :most-recent-sampling-date, most_recent_sampling_date),
output_file_url =             IF(:output-file-url IS NOT NULL, :output-file-url, output_file_url)

-- :name insert-attribute :! :n
-- :doc Insert an attribute

INSERT INTO continuous_tree_attributes (id, attribute_name)
VALUES (:id, :attribute-name)
ON DUPLICATE KEY UPDATE
id = :id,
attribute_name = :attribute-name

-- :name get-attributes :? :*
-- :doc Get attributes by tree-id

SELECT attribute_name
FROM continuous_tree_attributes
WHERE :id = id

-- :name get-tree :? :1
-- :doc Get entity by id

SELECT
analysis.id,
user_id,
created_on,
readable_name,
status,
progress,
tree_file_url,
tree_file_name,
x_coordinate_attribute_name,
y_coordinate_attribute_name,
timescale_multiplier,
most_recent_sampling_date,
output_file_url
FROM continuous_tree
JOIN analysis ON analysis.id = continuous_tree.id
WHERE :id = analysis.id
