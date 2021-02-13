-- :name upsert-time-slicer :! :n
-- :doc Upsert a time-slicer entity

INSERT INTO time_slicer(
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

-- :name update-time-slicer :! :n
-- :doc Updates a time-slicer entity

UPDATE time_slicer
SET
status = :status,
readable_name = IF(:readable-name IS NOT NULL, :readable-name, readable_name),
burn_in = IF(:burn-in IS NOT NULL, :burn-in, burn_in),
number_of_intervals = IF(:number-of-intervals IS NOT NULL, :number-of-intervals, number_of_intervals),
trait_attribute_name = IF(:trait-attribute-name IS NOT NULL, :trait-attribute-name, trait_attribute_name),
relaxed_random_walk_rate_attribute_name = IF(:relaxed-random-walk-rate-attribute-name IS NOT NULL, :relaxed-random-walk-rate-attribute-name, relaxed_random_walk_rate_attribute_name),
contouring_grid_size = IF(:contouring-grid-size IS NOT NULL, :contouring-grid-size, contouring_grid_size),
hpd_level = IF(:hpd-level IS NOT NULL, :hpd-level, hpd_level),
timescale_multiplier = IF(:timescale-multiplier IS NOT NULL, :timescale-multiplier, timescale_multiplier),
most_recent_sampling_date = IF(:most-recent-sampling-date IS NOT NULL, :most-recent-sampling-date, most_recent_sampling_date),
output_file_url = IF(:output-file-url IS NOT NULL, :output-file-url, output_file_url),
trees_count = IF(:trees-count IS NOT NULL, :trees-count, trees_count)
WHERE id = :id

-- :name get-time-slicer :? :1
-- :doc Get entity by id

SELECT
id,
user_id,
trees_file_url,
slice_heights_file_url,
status,
readable_name,
burn_in,
number_of_intervals,
trait_attribute_name,
relaxed_random_walk_rate_attribute_name,
contouring_grid_size,
hpd_level,
timescale_multiplier,
most_recent_sampling_date,
output_file_url,
trees_count
FROM time_slicer
WHERE :id = id

-- :name insert-attribute :! :n
-- :doc Insert an attribute

INSERT INTO time_slicer_attributes (time_slicer_id, attribute_name)
VALUES (:time-slicer-id, :attribute-name)
ON DUPLICATE KEY UPDATE
time_slicer_id = :time-slicer-id,
attribute_name = :attribute-name

-- :name get-attributes :? :*
-- :doc Get attributes by tree-id

SELECT attribute_name
FROM time_slicer_attributes
WHERE :time-slicer-id = time_slicer_id

-- :name delete-time-slicer :! :n
-- :doc Delete a entity by id

DELETE
FROM time_slicer
WHERE id = :id

-- :name get-status :? :1
-- :doc Get analysis status by id

SELECT
id,
status
FROM time_slicer
WHERE :id = id
