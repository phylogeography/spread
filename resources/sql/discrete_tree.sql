-- :name upsert-tree :! :n
-- :doc Upsert a discrete tree

INSERT INTO discrete_tree(
id,
tree_file_url,
locations_file_url,
locations_attribute_name,
timescale_multiplier,
most_recent_sampling_date,
output_file_url
)
VALUES (
:id,
:tree-file-url,
:locations-file-url,
:locations-attribute-name,
:timescale-multiplier,
:most-recent-sampling-date,
:output-file-url
)
ON DUPLICATE KEY UPDATE
tree_file_url =             IF(:tree-file-url IS NOT NULL, :tree-file-url, tree_file_url),
locations_file_url =        IF(:locations-file-url IS NOT NULL, :locations-file-url, locations_file_url),
locations_attribute_name =  IF(:locations-attribute-name IS NOT NULL, :locations-attribute-name, locations_attribute_name),
timescale_multiplier =      IF(:timescale-multiplier IS NOT NULL, :timescale-multiplier, timescale_multiplier),
most_recent_sampling_date = IF(:most-recent-sampling-date IS NOT NULL, :most-recent-sampling-date, most_recent_sampling_date),
output_file_url =           IF(:output-file-url IS NOT NULL, :output-file-url, output_file_url)

-- :name insert-attribute :! :n
-- :doc Insert an attribute

INSERT INTO discrete_tree_attributes (id, attribute_name)
VALUES (:id, :attribute-name)
ON DUPLICATE KEY UPDATE
id = :id,
attribute_name = :attribute-name

-- :name get-attributes :? :*
-- :doc Get attributes by tree-id

SELECT attribute_name
FROM discrete_tree_attributes
WHERE :id = id

-- :name get-tree :? :1
-- :doc Get entity by id

SELECT
analysis.id,
user_id,
created_on,
tree_file_url,
locations_file_url,
locations_attribute_name,
timescale_multiplier,
most_recent_sampling_date,
output_file_url,
readable_name,
status,
progress
FROM discrete_tree
JOIN analysis ON analysis.id = discrete_tree.id
WHERE :id = analysis.id
