-- :name upsert-tree :! :n
-- :doc Upsert a continuous tree

insert into continuous_tree(
tree_id,
user_id,
tree_file_path,
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
:tree-file-path,
:x-coordinate-attribute-name,
:y-coordinate-attribute-name,
:hpd-level,
:has-external-annotations,
:timescale-multiplier,
:most-recent-sampling-date
)
on duplicate key update
most_recent_sampling_date = :x-coordinate-attribute-name,
most_recent_sampling_date = :y-coordinate-attribute-name,
most_recent_sampling_date = :hpd-level,
most_recent_sampling_date = :has-external-annotations,
most_recent_sampling_date = :timescale-multiplier,
most_recent_sampling_date = :most-recent-sampling-date

-- :name delete-tree :! :n
-- :doc Delete a tree by id
delete
from continuous_tree
where tree_id = :tree-id
