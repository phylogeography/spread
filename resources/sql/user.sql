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

-- :name get-user-by-email :? :1
-- :doc find user by email
SELECT
id,
email
FROM user
WHERE email = :email
