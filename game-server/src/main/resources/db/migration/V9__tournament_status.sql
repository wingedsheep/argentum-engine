-- Tournaments used to be written to this table only when they finished, so abandoned or still-running
-- tournaments never appeared in the admin dashboard or player profiles. They are now recorded from the
-- moment the bracket goes live and updated as they progress, so a status column distinguishes the three
-- lifecycle states. Every pre-existing row was, by definition, only ever written on completion, so the
-- default backfills them as COMPLETED. IN_PROGRESS rows carry a started_at and a null ended_at until
-- they finish (COMPLETED) or the lobby is torn down early (ABANDONED).
ALTER TABLE tournaments ADD COLUMN status TEXT NOT NULL DEFAULT 'COMPLETED';

-- ended_at is now null while a tournament is in progress; readers coalesce it with started_at for
-- ordering and display. Drop the now() default too, so an in-progress insert genuinely stores null.
ALTER TABLE tournaments ALTER COLUMN ended_at DROP NOT NULL;
ALTER TABLE tournaments ALTER COLUMN ended_at DROP DEFAULT;

CREATE INDEX idx_tournaments_status ON tournaments (status);
