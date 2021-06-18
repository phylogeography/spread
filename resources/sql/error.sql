-- :name insert-error :! :n
-- :doc Insert an error

INSERT INTO error(
id,
error
)
VALUES (
:id,
:error
)

-- :name get-error :? :1
-- :doc Returns error by analysis id

SELECT
id,
error
FROM error
WHERE id = :id
