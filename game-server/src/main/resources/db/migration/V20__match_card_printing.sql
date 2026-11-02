-- The printing each recorded deck card was played as ("SET-CN", e.g. "M10-146"), so the deck viewers
-- show the art the player actually had — a drafted pool's printing, a chosen alternate art — instead
-- of the card's default printing. Null for unpinned cards and for every game recorded before this.
ALTER TABLE match_participant_cards ADD COLUMN printing TEXT;
