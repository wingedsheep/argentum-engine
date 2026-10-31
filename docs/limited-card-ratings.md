# Limited card ratings

How the engine AI decides how good a card is when it drafts, builds a limited deck, or generates a
deck for a game. Everything here lives in the `ai` module; the server only passes cards and set codes
in.

## The scale

Every rating is a number on Draftsim's 0–5 pick scale:

| Rating | Meaning |
|--------|---------|
| ≥ 3.9 | Bomb — the scorer's `BOMB` cutoff; anchors colour choice and gets splashed |
| 3.0–3.8 | Premium removal, strong evasive threats, top uncommons |
| 2.2–2.9 | Solid playables and filler |
| 1.0–2.0 | Marginal — situational, sideboard, or narrow |
| ≤ 1.0 | Unplayable in limited |

The scale is **absolute, not per-set**: the scorer subtracts a fixed quality baseline (`QJ = 2`) when
it weighs colours and treats anything at `BOMB = 3.9` or above as a bomb
(`DraftsimScorer`). A table that is internally consistent but rated low across the board would starve
colour weighting and bomb anchoring, so every table is calibrated to the same distribution: median
around 2.2–2.6, p90 around 3.2–3.6, a handful of cards at or above 3.9, unplayables near 0.7–1.0.

## Where a rating comes from

Three sources, best first.

### 1. Draftsim set tables — `ai/src/main/resources/draftai/`

```
draftai/ratings/<CODE>.json      { nameKey: rating }
draftai/ratings/_manifest.json   the set codes "all sets" covers
draftai/removal/<CODE>.json      { "Card Name": ["removal", "creature-removal"] }
draftai/removal/_overrides.json  hand-checked removal additions per set
draftai/archetypes/<CODE>.json   { nameKey: { archetypes: [{archetype, role}], fixing, splashable } }
```

`DraftsimData` loads them per set and caches them; a pool spanning several sets gets the union
(first set wins for a rating, removal is unioned). Keys are `DraftsimData.nameKey(name)`: the front
face of a `//` name, diacritics stripped, lowercased.

The tables have two provenances, documented on `DraftsimData`:

- **Vendored** — extracted from Draftsim's live draft-app bundle by `just draftsim-refresh`
  (`scripts/draftsim/`; `check` diffs, `write` writes). These reflect Draftsim's own pick data and
  are preferred whenever Draftsim covers a set: when the bundle starts covering a first-party set,
  its vendored table replaces ours.
- **First-party** — written here for sets Draftsim never covered. A card already rated in another
  shipped table keeps that value; every other card is rated by reading its oracle text against its
  own set's limited environment, then the whole table is checked against the calibration above. Each
  key is validated against the set's Scryfall booster pool, and the set gets a removal table too. A
  vendored refresh must never overwrite these.

Archetype tables exist only for sets whose upstream data carries archetype columns. With them the
scorer takes its archetype-aware path (`scoreBooster`); without them, the colour-bias path
(`scoreBoosterFallback`). `scoreBoosterAuto` picks.

### 2. 17Lands win rates — `rules-engine/src/main/resources/ratings/`

Measured game-in-hand win rates for the sets listed in that directory's `_manifest.json`.
`LimitedCardRater` maps a win rate onto the 0–5 scale (0.50 → 2.5, 0.55 → 3.5, 0.62+ → 5.0) and
uses it only when it has at least 50 games behind it.

### 3. The effect heuristic — `LimitedCardRater.heuristicRate`

For a card no table rates, a structural estimate from its `CardDefinition`:

- **Base** — creatures by stat efficiency (power + toughness per mana), planeswalkers at 3.0, other
  spells by type (instant 2.2, sorcery 2.0, …).
- **Keywords** — evasion and combat keywords add value.
- **Effects** — `EffectWalker` walks the script and sums what each effect is worth: removal (exile
  above destroy above bounce), damage by amount, card draw, tokens, pumps, control magic, counters.
  A composite is capped, a "may" is discounted.
- **Rarity and curve** — small nudges (mythic +0.8, a 2-drop +0.3, a 6-drop −0.2).

It reads what a card *does*, so it tells removal from a vanilla body, but it can't see synergy or
how a format plays. It is the floor, not the goal.

## Who reads what

| Consumer | Ratings used |
|----------|--------------|
| AI draft picks (`EngineAiPlayerController.chooseDraftPick`, Winston, grid) | Draftsim tables for the pack's sets, with fallback ratings (below) |
| Draftsim deckbuild (`DraftsimDeckBuildAdvisor`) — AI quick-game sealed decks, the human Auto-build | Same |
| AI deck from a tournament pool (`HeuristicDeckBuilder`) | `LimitedCardRater.rate` |
| "Draftsim" Suggest Pick advisor | Draftsim tables only (rarity ladder for unrated cards) |
| "Heuristic" Suggest Pick advisor (`LimitedPickScorer`) | Rarity and oracle-text keywords — no tables |
| Constructed and Commander generators (`ConstructedRatings`) | Every manifest table merged, cube tables excluded |

`LimitedCardRater.rate` chains the three sources: 17Lands, then any manifest table (so a reprint
inherits its rating from wherever it was rated), then the heuristic.

### Fallback ratings

`DraftsimSetTables.withFallbackRatings(cards)` fills every nonland card the set's table leaves
unrated with `LimitedCardRater.rate`. Without it the Draftsim scorer rates an unrated card off a
rarity ladder (mythic 4.5, rare 3.5, uncommon 2.5, common 1.5), so in a set with no table every
common ties with every other common and a weak rare outranks the best common removal. Lands are left
to Draftsim's own land handling.

Once a table has *any* ratings, the scorer and deckbuilder switch to their ratings-aware paths, which
cost several times more per build than the rarity-only path. That is the normal cost for every rated
set.

## Seeing a card's rating

The Assay explorer's card page (Set Completion → Assay Explorer tab) has a **How the AI rates it**
panel: the rating on this scale with its source, each set's starting number for the draft scorer, and
the heuristic's terms as a waterfall — shown even when a table outranks it, so a disagreement is
visible. `just ai-card "<name>"` prints the same in the terminal. A set's **AI gaps** tab on Set
Completion shows how much of the set its pick table covers and how many cards have no draft rating at
all. `LimitedCardRater.explain` is the one code path behind all three; `rate` is its answer.

## Adding or refreshing ratings

1. **Draftsim covers the set** — `just draftsim-refresh check`, review the diff, then
   `just draftsim-refresh write`. The scripts never write first-party tables, the removal overrides
   or `archetypes/FDN.json`; if a first-party set is now covered, drop it from `DraftsimData`'s
   first-party list.
2. **It doesn't** — write a first-party table:
   - list the set's booster pool from the Scryfall cache (`~/.cache/scryfall/<code>.json`,
     `draft_names`), basics excluded;
   - carry over ratings the card already has in any manifest table;
   - rate the rest from oracle text, then compare the table's median, p90 and count at or above 3.9
     with the calibration above and adjust;
   - write `ratings/<CODE>.json` (sorted, nameKey keys), `removal/<CODE>.json`, and add the code to
     `_manifest.json`;
   - add the code to `DraftsimData`'s first-party list and to `DraftsimDataTest`'s name-join test.

   The `set-loop` finishing PR does this for every draftable set it completes that has no table yet.

`DraftsimDataTest` fails when a manifest code has an empty table, when a key isn't already
nameKey-normalized, or when our registry's cards for a listed set don't resolve a rating.

Ratings are data, not AI behaviour, so a ratings PR doesn't add an `ai/CHANGELOG.md` line.

## Related

- [`draft-ai-port/SPEC_scoring.md`](draft-ai-port/SPEC_scoring.md) — the Draftsim pick scorer this
  port follows, constant by constant.
- [`draft-ai-port/SPEC_deckbuild.md`](draft-ai-port/SPEC_deckbuild.md) — the Draftsim deckbuilder.
- [`ai/architecture.md`](ai/architecture.md) — the engine AI as a whole.
