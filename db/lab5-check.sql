-- psql -v username=lab5postman-... -f db/lab5-check.sql
-- Check after login, rotation and replay; token contents are not printed.
SELECT id, username, device_id, status,
       access_token_expiry, refresh_token_expiry,
       left(md5(access_token), 12) AS access_fingerprint,
       left(md5(refresh_token), 12) AS refresh_fingerprint
FROM delivery.user_sessions
WHERE username = :'username'
ORDER BY refresh_token_expiry, id;
