-- The account's chosen avatar: the id of one of the preset portraits the client ships (see
-- `Avatars.kt`), or null for the default — the display name's initial in a circle. Runs only when
-- accounts are enabled.

ALTER TABLE users ADD COLUMN avatar TEXT;
