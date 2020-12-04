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
most_recent_sampling_date
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
:most-recent-sampling-date
)
on duplicate key update
x_coordinate_attribute_name = :x-coordinate-attribute-name,
y_coordinate_attribute_name = :y-coordinate-attribute-name,
hpd_level = :hpd-level,
has_external_annotations = :has-external-annotations,
timescale_multiplier = :timescale-multiplier,
most_recent_sampling_date = :most-recent-sampling-date

-- :name delete-tree :! :n
-- :doc Delete a tree by id
delete
from continuous_tree
where tree_id = :tree-id

-- :name insert-attribute :! :n
-- :doc Insert an attribute 

insert ignore into continuous_tree_attributes (tree_id, attribute_name)
values (:tree-id, :attribute-name)
