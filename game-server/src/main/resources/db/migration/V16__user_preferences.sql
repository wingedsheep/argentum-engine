-- Player preferences (auto-pass defaults, battlefield stacking, motion, …), per account. Guests keep
-- the same JSON in localStorage; a signed-in user's copy lives here so their table setup follows them
-- across devices. Like `learn_progress`, the body is the client's document stored verbatim — the
-- server never interprets it.
ALTER TABLE users ADD COLUMN preferences TEXT;
