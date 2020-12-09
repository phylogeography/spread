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
