# Innistrad: Crimson Vow (VOW) — Mechanics

Counts are over the 272 booster cards (excluding basic lands and tokens), detected by
regex over front-face + back-face oracle text, so they are **approximate** — a payoff that
merely mentions "Blood token" is counted alongside the cards that make one. "Unimpl" = not
yet implemented as of this document (~77 cards done; see [`cards.md`](cards.md) for the live
checklist). Counts predate the in-flight card batch and will drift.

Engine-support column reflects whether the SDK/rules-engine already models the mechanic (so
cards using *only* supported mechanics need **no backend change** — pure `add-card` work).

## Set mechanics

| Mechanic | Cards | Unimpl | Engine support | Notes |
|----------|------:|-------:|----------------|-------|
| **Blood token** | ~30 | ~19 | ✅ `Effects.CreateBlood` | Artifact token: `{1}, {T}, Discard a card, Sacrifice: Draw a card.` 11 already done (e.g. Blood Fountain, Voldaren Epicure). |
| **Cleave** | 12 | 12 | ❌ **GAP → [#1259](https://github.com/wingedsheep/argentum-engine/issues/1259)** | CR 702.148 — text-modifying alternative cost on instants/sorceries. Implement as a cast-mode branch, not string mutation (see issue). Canonical: Dread Fugue. |
| **Training** | 9 | 9 | ❌ **GAP → [#1261](https://github.com/wingedsheep/argentum-engine/issues/1261)** | CR 702.149 — attack trigger gated on a co-attacker with greater power → +1/+1 counter; plus a "when this creature trains" payoff hook (702.149c). Structural analog: Mentor + Decayed. |
| **Exploit** | 9 | 9 | ❌ **GAP → [#1260](https://github.com/wingedsheep/argentum-engine/issues/1260)** | CR 702.110 — ETB "may sacrifice a creature" + a paired "when this creature exploits a creature" payoff (702.110b, the crux). Analog: Casualty's reflexive "when you do" trigger. Also surfaces as blocked trigger `WhenAPermanentExploitsAPermanent` (×9). |
| **Disturb** | ~13 | ~13 | ⚠️ **GAP — no issue yet** | CR 702.146 — cast the back face from your graveyard, then exile. Transform machinery exists (`TransformEffects`), but there is no Disturb keyword or graveyard-cast-back-face DSL. All are DFCs (spirit front // enchantment or aura back). Not on the coverage leaderboard because these DFCs sit in the tool's "unmatched in mtgish" bucket. |
| **Daybound / Nightbound** | ~14 | ~14 | ⚠️ **GAP — no issue yet** | CR 702.145 — day/night designation + werewolf-style DFC flips keyed on spells-cast-per-turn. **Zero** engine support: no day/night state tracker anywhere in main source, no keyword. All are DFCs. Also invisible to the coverage leaderboard (unmatched-in-mtgish bucket). |

**Transform / DFC machinery** — ✅ present (`TransformEffect`, `ExileAndReturnTransformedEffect`,
`ReturnSelfFromZoneTransformedEffect`). The *effect* layer that flips a permanent's face exists and is
reused by other sets; what VOW is missing is the two *keyword layers* above (Disturb's graveyard-cast +
Daybound/Nightbound's day/night trigger) that drive those flips.

## Evergreen / returning keywords present

Flying (~30), Menace (~8), Defender (~5), Reach (~4), Vigilance (~4), Flash (~4),
Lifelink (~3), Deathtouch (~3), Haste (~3), First strike (~3), Ward (~2),
Double strike (1), Hexproof (1). Plus Equip (~5), Mill (~8), Scry (~1), Investigate,
Fight (~1), and +1/+1 counters (~29). **All engine-supported.**

## Backend-change assessment

Three headline VOW mechanics have open engine work items (route through `add-feature`):

- **Cleave** ([#1259](https://github.com/wingedsheep/argentum-engine/issues/1259), CR 702.148) —
  ×12 cards. New alternative-cost type + cast-mode condition; behaviour branch, not text mutation.
- **Training** ([#1261](https://github.com/wingedsheep/argentum-engine/issues/1261), CR 702.149) —
  ×9 cards. Attack trigger + power comparison + "when it trains" hook.
- **Exploit** ([#1260](https://github.com/wingedsheep/argentum-engine/issues/1260), CR 702.110) —
  ×9 cards (+ the `WhenAPermanentExploitsAPermanent` trigger, ×9). ETB optional sacrifice + paired
  "when it exploits" payoff.

These three are exactly the top entries on `just coverage-gaps --set VOW`'s BLOCKED leaderboard
(Cleave ×12, Training ×9, Exploit ×9), so clearing them unlocks the most cards.

**Two further gaps have no work item yet** — flagged here so they aren't mistaken for supported:

- **Disturb** (CR 702.146, ~13 cards) — needs a graveyard-cast-back-face keyword layered on the
  existing transform machinery.
- **Daybound / Nightbound** (CR 702.145, ~14 cards) — needs a day/night game-state tracker plus the
  werewolf-flip trigger; nothing exists today.

Both are absent from the coverage leaderboard only because their DFCs land in the tool's
"unmatched in mtgish" bucket (name-join misses), **not** because they're covered.

**Smaller blocked capabilities** (from `coverage-gaps`, lower-volume — assess per card, may be pure
`add-feature` one-offs): `SetPT` layer effect (×4), `RemoveCounters` cost/action (×3),
`WhenAPlayerPlaysALand` trigger (×2), `PermanentDoesntUntapDuringControllersNextUntap` (×2),
characteristic-defining `CDA_Power`/`CDA_Toughness` (×2 each), `AddCardtype` (×2), and a tail of ×1
items.

**Everything else is pure `add-card` authoring** — Blood-token cards, the returning/evergreen keyword
cards, and standard effects. Suggested order: clear the Blood/evergreen single-faced cards first, then
land the Cleave/Training/Exploit features (highest unlock), and defer the Disturb / Daybound DFCs until
those two keyword layers are built.
