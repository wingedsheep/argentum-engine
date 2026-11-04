-- Which recorded results count toward win/loss stats.
--
-- A seat that never took an action past the mulligan — a tournament no-show whose disconnect timer
-- conceded for them, or a player who kept and conceded straight away — didn't play the game, so its
-- loss (and the free win it hands the other side) says nothing about anyone's play.
--
--   took_action     - the seat made at least one play of its own after the opening hands: a land, a
--                     spell, an ability, a real attack or block, a decision. Passing priority and empty
--                     combat declarations don't count: the server submits those for an idle seat. Null
--                     for games recorded before this column existed and never backfilled (no replay),
--                     which read as "took an action" so old stats stay as they were.
--   counts_in_stats - this seat's result counts: it took an action, and so did at least one other seat.
--                     Every win/loss figure filters on it; game lists still show the row, marked.
ALTER TABLE match_participants ADD COLUMN took_action BOOLEAN;
ALTER TABLE match_participants ADD COLUMN counts_in_stats BOOLEAN NOT NULL DEFAULT true;

-- Tournament matches decided because a player left the event, never played. Counted inside
-- wins/losses (the standings are built on them), and stored separately so a record can leave them out.
ALTER TABLE tournament_participants ADD COLUMN forfeit_wins   INT NOT NULL DEFAULT 0;
ALTER TABLE tournament_participants ADD COLUMN forfeit_losses INT NOT NULL DEFAULT 0;
