-- :name upsert-tree :! :n
-- :doc Upsert a continuous tree

insert into continuous_tree(
tree_id,
user_id,
tree_file_url,
x_coordinate_attribute_name,
y_coordinate_attribute_name,
hpd_level,
has_external_annotations,
timescale_multiplier,
most_recent_sampling_date,
status,
output_file_url
)
values (
:tree-id,
:user-id,
:tree-file-url,
:x-coordinate-attribute-name,
:y-coordinate-attribute-name,
:hpd-level,
:has-external-annotations,
:timescale-multiplier,
:most-recent-sampling-date,
:status,
:output-file-url
)
on duplicate key update
x_coordinate_attribute_name = :x-coordinate-attribute-name,
y_coordinate_attribute_name = :y-coordinate-attribute-name,
hpd_level = :hpd-level,
has_external_annotations = :has-external-annotations,
timescale_multiplier = :timescale-multiplier,
most_recent_sampling_date = :most-recent-sampling-date,
status = :status,
output_file_url = :output-file-url

-- :name delete-tree :! :n
-- :doc Delete a tree by id
delete
from continuous_tree
where tree_id = :tree-id

-- :name insert-attribute :! :n
-- :doc Insert an attribute

insert ignore into continuous_tree_attributes (tree_id, attribute_name)
values (:tree-id, :attribute-name)

-- :name insert-hpd-level :! :n
-- :doc Insert an hpd level

insert ignore into continuous_tree_hpd_levels (tree_id, level)
values (:tree-id, :level)

-- :name get-attributes :? :*
-- :doc Get attributes by tree-id

select attribute_name
from continuous_tree_attributes
where :tree-id = tree_id

-- :name get-hpd-levels :? :*
-- :doc Get hpd-levels by tree-id

select level
from continuous_tree_hpd_levels
where :tree-id = tree_id

-- :name get-tree :? :1
-- :doc Get entity by id

select
tree_id,
user_id,
tree_file_url,
x_coordinate_attribute_name,
y_coordinate_attribute_name,
hpd_level,
has_external_annotations,
timescale_multiplier,
most_recent_sampling_date,
status,
output_file_url
from continuous_tree
where :tree-id = tree_id
