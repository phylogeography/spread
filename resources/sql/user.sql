-- :name upsert-user :! :n
-- :doc Upsert a user
INSERT INTO user(
id,
email
)
VALUES (
:id,
:email
)
ON DUPLICATE KEY UPDATE
email = :email

-- :name get-user-by-id :? :1
-- :doc find user by id
SELECT
id,
email
FROM user
WHERE id = :id

-- :name get-user-by-email :? :1
-- :doc find user by email
SELECT
id,
email
FROM user
WHERE email = :email

-- :name count-user-analysis :? :1
-- :doc Return total number of all user analysis

SELECT count(*) AS total_count FROM
(SELECT id, user_id, readable_name, created_on, progress, status, (SELECT "continuous_tree") AS of_type FROM continuous_tree
JOIN continuous_tree_status ON continuous_tree_status.tree_id = continuous_tree.id
WHERE user_id = :user-id
UNION
SELECT id, user_id, readable_name, created_on, progress, status, (SELECT "discrete_tree") AS of_type FROM discrete_tree
JOIN discrete_tree_status ON discrete_tree_status.tree_id = discrete_tree.id
WHERE user_id = :user-id
UNION
SELECT id, user_id, readable_name, created_on, progress, status, (SELECT "bayes_factor_analysis") FROM bayes_factor_analysis
JOIN bayes_factor_analysis_status ON bayes_factor_analysis_status.bayes_factor_analysis_id = bayes_factor_analysis.id
WHERE user_id = :user-id
UNION
SELECT id, user_id, readable_name, created_on, progress, status, (SELECT "time_slicer") FROM time_slicer
JOIN time_slicer_status ON time_slicer_status.time_slicer_id = time_slicer.id
WHERE user_id = :user-id ORDER BY created_on) AS all_user_analysis

-- :name search-user-analysis :? :*
-- :doc Return all user analysis

SELECT id, user_id, readable_name, created_on, progress, status, (SELECT "continuous_tree") AS of_type FROM continuous_tree
JOIN continuous_tree_status ON continuous_tree_status.tree_id = continuous_tree.id
WHERE user_id = :user-id
UNION
SELECT id, user_id, readable_name, created_on, progress, status, (SELECT "discrete_tree") AS of_type FROM discrete_tree
JOIN discrete_tree_status ON discrete_tree_status.tree_id = discrete_tree.id
WHERE user_id = :user-id
UNION
SELECT id, user_id, readable_name, created_on, progress, status, (SELECT "bayes_factor_analysis") FROM bayes_factor_analysis
JOIN bayes_factor_analysis_status ON bayes_factor_analysis_status.bayes_factor_analysis_id = bayes_factor_analysis.id
WHERE user_id = :user-id
UNION
SELECT id, user_id, readable_name, created_on, progress, status, (SELECT "time_slicer") FROM time_slicer
JOIN time_slicer_status ON time_slicer_status.time_slicer_id = time_slicer.id
WHERE user_id = :user-id
ORDER BY created_on
LIMIT :limit
OFFSET :offset
