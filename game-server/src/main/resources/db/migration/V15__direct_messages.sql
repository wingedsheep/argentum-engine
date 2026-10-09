-- Direct messages between accounts.
--
-- One thread per pair of accounts, stored once with the two ids in a canonical order (user_low is the
-- id whose text form sorts first). A thread opened by someone who isn't the other person's friend is a
-- *message request* for the recipient until they accept it (accepted = true); friends skip the
-- request step. While a request is pending the initiator can only send a few messages.
--
-- Each side keeps its own read marker (*_read_at, for unread counts) and clear marker (*_cleared_at):
-- deleting a conversation or declining a request only hides the messages *you* can see — anything
-- older than your clear marker — and the thread reappears for you if a new message arrives. Blocking
-- (user_blocks) stops new messages either way. Runs only when accounts are enabled.

CREATE TABLE dm_threads (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_low        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    user_high       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    initiator_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    accepted        BOOLEAN     NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_message_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    low_read_at     TIMESTAMPTZ,
    high_read_at    TIMESTAMPTZ,
    low_cleared_at  TIMESTAMPTZ,
    high_cleared_at TIMESTAMPTZ,
    CONSTRAINT dm_threads_distinct    CHECK (user_low <> user_high),
    CONSTRAINT dm_threads_unique_pair UNIQUE (user_low, user_high)
);
CREATE INDEX idx_dm_threads_low  ON dm_threads (user_low);
CREATE INDEX idx_dm_threads_high ON dm_threads (user_high);

CREATE TABLE dm_messages (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id  UUID        NOT NULL REFERENCES dm_threads (id) ON DELETE CASCADE,
    sender_id  UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_dm_messages_thread_time ON dm_messages (thread_id, created_at DESC);
