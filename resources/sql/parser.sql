-- :name get-status :? :1
-- :doc Return a status of a single parser by id

SELECT id, user_id, readable_name, created_on, progress, status, of_type FROM
(SELECT id, user_id, readable_name, created_on, progress, status, (SELECT "CONTINUOUS_TREE") AS of_type FROM continuous_tree JOIN continuous_tree_status ON continuous_tree_status.tree_id = continuous_tree.id
UNION
SELECT id, user_id, readable_name, created_on, progress, status, (SELECT "DISCRETE_TREE") AS of_type FROM discrete_tree JOIN discrete_tree_status ON discrete_tree_status.tree_id = discrete_tree.id
UNION
SELECT id, user_id, readable_name, created_on, progress, status, (SELECT "BAYES_FACTOR_ANALYSIS") AS of_type FROM bayes_factor_analysis JOIN bayes_factor_analysis_status ON bayes_factor_analysis_status.bayes_factor_analysis_id = bayes_factor_analysis.id) AS q1
WHERE q1.id = :parser-id;
