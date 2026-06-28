-- Switch the account primary key from a BIGINT identity to a UUID.
--
-- Why: the account id is now a shareable, non-guessable handle — you invite a friend by handing them
-- your account id, never your email (see V5). Game/replay ids were already UUIDs; only users.id was a
-- BIGINT. This backfills every existing row and re-points each foreign key, so no account, deck, login
-- token, or match-history association is lost. Like the other migrations it only runs when accounts
-- are enabled.

-- gen_random_uuid() is built in on PostgreSQL 13+; the extension is a safety net for older servers.
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. New UUID identity on users, backfilled for every existing row.
ALTER TABLE users ADD COLUMN uuid UUID NOT NULL DEFAULT gen_random_uuid();

-- 2. UUID foreign-key columns on every child, backfilled from the join on the old BIGINT id.
ALTER TABLE login_tokens            ADD COLUMN user_uuid UUID;
ALTER TABLE decks                   ADD COLUMN user_uuid UUID;
ALTER TABLE match_participants      ADD COLUMN user_uuid UUID;
ALTER TABLE tournament_participants ADD COLUMN user_uuid UUID;

UPDATE login_tokens            c SET user_uuid = u.uuid FROM users u WHERE c.user_id = u.id;
UPDATE decks                   c SET user_uuid = u.uuid FROM users u WHERE c.user_id = u.id;
UPDATE match_participants      c SET user_uuid = u.uuid FROM users u WHERE c.user_id = u.id;
UPDATE tournament_participants c SET user_uuid = u.uuid FROM users u WHERE c.user_id = u.id;

-- 3. Drop every foreign key that references users(id), looked up dynamically so we don't depend on
--    auto-generated constraint names.
DO $$
DECLARE r RECORD;
BEGIN
    FOR r IN
        SELECT con.conname AS constraint_name, rel.relname AS table_name
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE con.contype = 'f'
          AND con.confrelid = 'users'::regclass
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', r.table_name, r.constraint_name);
    END LOOP;
END $$;

-- 4. Swap users' primary key to the UUID column. Dropping the old id column also drops its identity
--    sequence and the (now FK-free) primary key.
ALTER TABLE users DROP COLUMN id;
ALTER TABLE users RENAME COLUMN uuid TO id;
ALTER TABLE users ADD PRIMARY KEY (id);
ALTER TABLE users ALTER COLUMN id SET DEFAULT gen_random_uuid();

-- 5. Point each child's user_id at the new UUID and restore its NOT NULL / index / FK
--    (dropping the old column also dropped its index, so recreate it).

-- login_tokens: NOT NULL, ON DELETE CASCADE
ALTER TABLE login_tokens DROP COLUMN user_id;
ALTER TABLE login_tokens RENAME COLUMN user_uuid TO user_id;
ALTER TABLE login_tokens ALTER COLUMN user_id SET NOT NULL;
CREATE INDEX idx_login_tokens_user ON login_tokens (user_id);
ALTER TABLE login_tokens ADD CONSTRAINT login_tokens_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- decks: NOT NULL, ON DELETE CASCADE
ALTER TABLE decks DROP COLUMN user_id;
ALTER TABLE decks RENAME COLUMN user_uuid TO user_id;
ALTER TABLE decks ALTER COLUMN user_id SET NOT NULL;
CREATE INDEX idx_decks_user ON decks (user_id);
ALTER TABLE decks ADD CONSTRAINT decks_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE;

-- match_participants: nullable (guests/AI), ON DELETE SET NULL
ALTER TABLE match_participants DROP COLUMN user_id;
ALTER TABLE match_participants RENAME COLUMN user_uuid TO user_id;
CREATE INDEX idx_match_participants_user ON match_participants (user_id);
ALTER TABLE match_participants ADD CONSTRAINT match_participants_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;

-- tournament_participants: nullable (guests/AI), ON DELETE SET NULL
ALTER TABLE tournament_participants DROP COLUMN user_id;
ALTER TABLE tournament_participants RENAME COLUMN user_uuid TO user_id;
CREATE INDEX idx_tournament_participants_user ON tournament_participants (user_id);
ALTER TABLE tournament_participants ADD CONSTRAINT tournament_participants_user_id_fkey
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL;
