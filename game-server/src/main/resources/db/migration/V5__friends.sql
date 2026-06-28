-- Friends and online-presence visibility.
--
-- A friendship is requested by one account and accepted by the other. A single directed row carries
-- both phases: status PENDING is an outstanding request (requester_id -> addressee_id); once the
-- addressee accepts it becomes status ACCEPTED, at which point the friendship is symmetric (queried in
-- both directions). Declining / cancelling / unfriending all delete the row.
--
-- Presence (who is online) is derived live from connected WebSocket sessions, not stored here. The
-- per-account hide_presence flag lets a player appear offline to everyone. Runs only when accounts are
-- enabled.

ALTER TABLE users ADD COLUMN hide_presence BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE friendships (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    addressee_id UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- PENDING: request awaiting the addressee. ACCEPTED: mutual friends.
    status       TEXT        NOT NULL DEFAULT 'PENDING',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    responded_at TIMESTAMPTZ,
    CONSTRAINT friendships_distinct    CHECK (requester_id <> addressee_id),
    CONSTRAINT friendships_unique_pair UNIQUE (requester_id, addressee_id)
);
CREATE INDEX idx_friendships_requester ON friendships (requester_id);
CREATE INDEX idx_friendships_addressee ON friendships (addressee_id);
