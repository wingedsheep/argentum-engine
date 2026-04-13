[x] Foothill guide can still be killed by combat damage from goblins
[x] Player can still be targeted by abilities when they control true believer
[x] Layer 7 controller-dependent filters not re-resolved after Layer 2 control change
[x] Portent of Calamity skips the "4+ exiled → free cast" clause and doesn't enforce one-per-card-type
[ ] Scrapshooter (and likely the whole Bloomburrow gift cycle) decides the gift at ETB instead of cast time (see `scrapshooter-gift-at-etb.md`)
[x] Cannot choose offspring (or kicker) when casting from exile or graveyard — `CastFromZoneEnumerator` never generates kicked/offspring variants, only `CastSpellEnumerator.enumerateKicker()` does (hand only)