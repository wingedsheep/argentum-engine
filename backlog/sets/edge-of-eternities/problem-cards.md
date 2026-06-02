https://scryfall.com/sets/eoe?order=name&as=grid
https://api.scryfall.com/cards/named?exact=Beyond%20the%20Quiet&set=eoe

# Problem Cards

## Status: cards still blocked on engine work (9: 2 small + 7 large)

The booster set is at **252 / 261**. The cards below are the only unimplemented ones; each is
blocked on a missing engine/SDK feature. The blocking clause and the engine change needed are
summarized here and detailed in [`missing-effects.md`](missing-effects.md) (section numbers in
parentheses).

Cards are split by the scope of the engine work each one requires. "Small" = a single focused
SDK/engine addition that composes with existing primitives. "Large" = multiple coupled features,
a new subsystem, or work that reaches across several engine layers.

### Small engine work (2)

| Card | Blocking clause | Engine change needed (missing-effects §) |
|------|-----------------|-------------------------------------------|
| Tannuk, Steadfast Second | "Artifact cards and red creature cards in your hand have warp {2}{R}" | Static that grants Warp (with cost) to filtered hand cards (§15) |
| Weftwalking | "The first spell each player casts ... may be cast without paying its mana cost" | First-spell-each-turn free-cast static, per-player gate (§20) |

### Large engine work (7)

| Card | Blocking clause | Engine change needed (missing-effects §) |
|------|-----------------|-------------------------------------------|
| Bioengineered Future | "Each creature you control enters with an additional +1/+1 counter ... for each land that entered ... this turn" | New turn tracker (lands ETB'd under you) + continuous extra-ETB-counters static targeting *other* creatures (§5) |
| Zero Point Ballad | "Destroy all creatures with toughness X or less" + reanimate one destroyed this way | Dynamic-toughness mass-destroy filter + batch tracking for "destroyed this way" reanimation (§10) |
| Lightstall Inquisitor | "each opponent exiles a card from their hand and may play that card ..." (+cost/tapped) | Opponent exile-from-hand granting may-play to *owner* + scoped cost increase + scoped lands-enter-tapped (§12) |
| Moonlit Meditation | "instead create that many tokens that are copies of enchanted permanent" (once/turn) | CreateToken replacement effect + once-per-turn gate + copy-of-enchanted token source (§13) |
| Terminal Velocity | put a permanent from hand and grant it haste + an LTB trigger + an end-step self-sac | Grant arbitrary bundle of abilities (incl. new triggered abilities) to a put-in permanent (§16) |
| Mm'menon, the Right Hand | "cast artifact spells from the top of your library" + restricted mana | Play-from-top-of-library static + restricted mana (non-hand only) + look-at-top-anytime (§19) |
| Tezzeret, Cruel Captain | −7 emblem; "it becomes a 0/0 Robot artifact creature" | Emblem creation with recurring combat trigger + "becomes 0/0 Robot artifact creature" type-set on a noncreature (§24) |
