# Brawl / Commander Draft Format

A drafted, singleton-flavoured 1v1 limited format. Players draft a pool from N boosters, pick one legendary from
that pool as their commander, and build a 60-card deck that fits the commander's color identity. Game-time uses the
`Format.Commander` engine config, with shorter life totals tuned for 1v1.

This is the "Brawl draft" sibling of the existing Sealed / Booster Draft / Winston / Grid lobby formats. It is
intentionally **1v1 only** for the foreseeable future — multiplayer commander is its own (much larger) project
([commander-format.md](commander-format.md) Phase 3).

## Status (2026-05-15)

Greenfield. Nothing has been built yet, but most of the prerequisites are in place:

- ✅ `BoosterGenerator.GuaranteedLegendaryBooster` strategy (`rules-engine/.../limited/BoosterGenerator.kt` →
  `mtg-sdk/.../limited/BoosterStrategy.kt:68`). Already does the "Dominaria slot": every booster has a legendary
  occupying the matching-rarity slot, falls back to base generation if the pool has no legendaries.
- ✅ Multi-set sealed pool generation with per-set distribution (`BoosterGenerator.generateSealedPool(setCodes,
  boosterCount)` and the `boosterDistribution: Map<String, Int>` overload at line 253). Each booster comes from a
  single set; no mid-pack mixing.
- ✅ `Format.Commander` engine config (`mtg-sdk/.../core/Format.kt`) — already parameterised on deck size, starting
  life, commander damage threshold, etc. A Brawl-shaped variant is config, not new code.
- ✅ `DeckFormat.BRAWL` / `STANDARD_BRAWL` enum entries + `isCommanderShape` helper
  (`mtg-sdk/.../core/DeckFormat.kt`).
- ✅ `TournamentLobby` supports the DRAFT state machine (`WAITING_FOR_PLAYERS → DRAFTING → DECK_BUILDING →
  TOURNAMENT_ACTIVE`) and a `deckFormat` field already exists on the lobby — currently only honoured for
  `PREMADE_DECKS`.
- ✅ Commander deck validator (`DeckValidator.validate(Deck, format)` with the `Deck.commander` field).

**Blockers**

- ⚠ **Engine commander runtime** ([commander-format.md](commander-format.md) Phase 1) — command zone setup,
  cast-from-command, commander tax, zone-change redirect, commander damage SBA. Brawl Draft can't actually *play*
  until this lands. Drafting + deck-building can ship independently and be exercised via Premade flow as a stopgap.

## Design principle

**Reuse the existing draft surface; add a Brawl-shaped booster strategy and a post-draft commander pick.** The
lobby state machine, pack-passing, deckbuilder, and tournament scheduler are all unchanged. The only new pieces are:

1. A `TournamentFormat.BRAWL_DRAFT` entry that wires the lobby to the right pack strategy and post-draft step.
2. A `BrawlDraftConfig` (analogue of how the lobby already holds `boosterCount` / `boosterDistribution`) that selects
   the booster strategy, pack count, mix mode, deck size, and starting life.
3. A new lobby state — `COMMANDER_SELECTION` — between `DRAFTING` and `DECK_BUILDING`, where each player picks one
   legendary from their drafted pool.
4. `Format.Commander` instantiated with Brawl-flavoured values (60 cards, 25 life) when the tournament starts.

**Locked decisions** (research basis: Commander Legends / Commander Masters draft templates — see the closing
"Research" section):

- **60-card decks, not 99.** A 1v1 draft pool cannot stretch to 99 unique cards without ballooning the pack count
  past playability. Commander Legends and Commander Masters both shrunk to 60.
- **Singleton rule dropped.** Multiple copies of any drafted card are allowed (basics aside, as always).
- **One legendary guaranteed per pack** — `GuaranteedLegendaryBooster` everywhere. This is non-negotiable; without
  it, ~10% of pools end up with no legal commander.
- **Chaos-style mixing across the selected sets** is the default. Each pack comes from a single set (no mid-pack
  mixing — already how `BoosterGenerator` works), and the set is chosen per-pack from the lobby's selected list.
- **5 packs per player** as the default for an 8-pack drafted experience. (4 = too thin for 60 cards once colors get
  cut; 6 = drags. 5 hits the sweet spot for ~100-card pools.) Host-configurable in the lobby.
- **Commander pick happens *after* the full draft**, not at P1P1. Players need to see their full pool before
  committing to colors — this is what Commander Masters did with its auto-partner rule, and the alternative locks
  you into your earliest legendary.

---

## Phase 1 — Format scaffolding + booster generation

Smallest shippable slice: a new `TournamentFormat`, a Brawl-shaped booster strategy choice, and an end-to-end draft
that produces a pool. No commander pick yet; deckbuilder treats the output as a regular sealed pool.

### 1.1 New `TournamentFormat` entry

- `game-server/.../lobby/TournamentLobby.kt:15` — add `BRAWL_DRAFT` to the `TournamentFormat` enum, between
  `GRID_DRAFT` and `PREMADE_DECKS`.
- `protocol/ServerMessage.kt` / `protocol/ClientMessage.kt` — extend any format-discriminated DTOs (the existing
  `TournamentFormat` is serialized over the wire).
- `web-client/src/types/messages.ts` — mirror the new enum value.

### 1.2 Lobby-level config

`TournamentLobby` already carries `boosterCount` and `boosterDistribution`. Add Brawl-specific knobs as constructor
fields with safe defaults:

```kotlin
var packsPerPlayer: Int = 5,             // BRAWL_DRAFT only; default 5 (~100-card pool)
var guaranteeLegendaryPerPack: Boolean = true,
var deckSizeOverride: Int? = null,       // null → 60 for BRAWL_DRAFT, 100 for COMMANDER variants
var startingLifeOverride: Int? = null,   // null → 25 (Brawl) by default
```

`boosterCount` continues to mean "packs per player" in DRAFT mode; `packsPerPlayer` is its Brawl-flavoured alias to
keep call sites obvious. (Or: just reuse `boosterCount` with a `5` default for `BRAWL_DRAFT`. Pick whichever is less
sprawling at PR time — leaning toward reusing `boosterCount` to avoid a parallel field.)

### 1.3 Brawl booster strategy wiring

The existing draft pipeline (`BoosterDraftHandler` → `TournamentLobby` → `BoosterGenerator.generateBooster`) calls
each set's configured `BoosterStrategy`. For Brawl Draft, override per-pack to `GuaranteedLegendaryBooster`.

- `game-server/.../config/GameBeansConfig.kt:71` — `BoosterGenerator` is built once from `MtgSet.toBoosterSetConfig`.
  The `SetConfig.boosterStrategy` is what gets used. For Brawl Draft we want to **override at generation time**, not
  per-set permanently. Two options:
  - **Option A (preferred): per-call strategy override.** Add `fun generateBooster(setCode: String, strategy:
    BoosterStrategy)` overload to `BoosterGenerator` and have the lobby call it with `GuaranteedLegendaryBooster`
    when `format == BRAWL_DRAFT && guaranteeLegendaryPerPack`. Keeps `SetConfig` immutable.
  - Option B: a `BoosterGenerator.withStrategy(BoosterStrategy)` builder that returns a wrapped instance for the
    lobby's lifetime. Heavier, more allocation, but fewer call-site changes.
- `BoosterDraftHandler.sendPackToPlayer` (`handler/BoosterDraftHandler.kt:148`) — generates packs via the lobby;
  thread the strategy through.

### 1.4 Empty-legendary-pool fallback

`GuaranteedLegendaryBooster` already falls back to base generation when a pool has no legendaries
(`BoosterStrategy.kt:73-75`). Verify with a unit test that booster generation still completes for sets with sparse
legendary printings (e.g., an old core set). No additional code; just a regression guard.

### 1.5 Definition of done (Phase 1)

- [ ] Host can create a lobby with `format = BRAWL_DRAFT`, choose ≥1 sets, `packsPerPlayer = 5` (configurable).
- [ ] Draft proceeds through 5 packs of 15 cards each, picking 1 at a time, passing left/right alternating.
- [ ] Every pack contains exactly one legendary (asserted in an integration test against BLB + MOM + DSK).
- [ ] After the last pick, lobby transitions to `DECK_BUILDING` with the full pool as `cardPool`.
- [ ] Player builds a 60-card deck via the existing deckbuilder; lobby starts a single 1v1 match.
- [ ] **Stopgap:** game runs under `Format.Standard` (no command zone yet) — Phase 3 wires the real commander
      engine. Phase 1 is "drafted singleton 60-card pile, ignoring the legendary slot."

---

## Phase 2 — Commander selection step

Between `DRAFTING` and `DECK_BUILDING`, each player picks one legendary creature (or planeswalker with "can be your
commander" override) from their drafted pool. The pick locks the deck's color identity.

### 2.1 New lobby state

- `TournamentLobby.kt:74` — add `COMMANDER_SELECTION` to `LobbyState`, between `DRAFTING` and `DECK_BUILDING`.
- State machine becomes:
  ```
  WAITING_FOR_PLAYERS → DRAFTING → COMMANDER_SELECTION → DECK_BUILDING → TOURNAMENT_ACTIVE → TOURNAMENT_COMPLETE
  ```
- Gate transition: when the last pack is empty, move to `COMMANDER_SELECTION` only for `BRAWL_DRAFT`. Other DRAFT
  formats continue straight to `DECK_BUILDING`.

### 2.2 Eligible-commander enumeration

- Reuse `game-server/.../deck/CommanderEligibility` (already exists per [commander-format.md](commander-format.md)
  Phase 1.7). It filters legendary creatures + planeswalkers with override + cards with explicit "can be your
  commander" oracle text.
- Apply it to `LobbyPlayerState.cardPool`. Send the filtered list to the client as the selectable pool.

### 2.3 Protocol

Two new messages:

| Direction | Message | Payload |
|---|---|---|
| S → C | `CommanderSelectionStarted` | `eligibleCommanders: List<CardId>` (per-player) |
| C → S | `SelectCommander` | `commanderName: String` |
| S → C | `CommanderSelected` (echo / opponent notification) | `playerId`, `commanderName` (opponent only sees count progress until reveal) |

Both players must select before the lobby advances to `DECK_BUILDING`. Standard ready-up pattern, reuse
`PlayerReadyForRound` mechanics.

### 2.4 Persist the commander

- `LobbyPlayerState` (`TournamentLobby.kt:93`) — add `var commander: String? = null`.
- On `DECK_BUILDING` entry, populate `Deck.commander` with the selected commander name; the deckbuilder picks it up
  automatically (commander-format Phase 1.8 work already plumbs `Deck.commander` end-to-end for validation).

### 2.5 The "no legendary in my pool" edge case

`GuaranteedLegendaryBooster` makes this rare but not impossible: a sparse pool can leave the legendary slot empty
(see 1.4). Handling:

- If the pool has zero eligible commanders → auto-assign **The Prismatic Piper** (precedent: Commander Legends).
  Requires `mtg-sets` to carry a Piper definition; if absent, mint a synthetic vanilla `1G, 2/3 Human` placeholder at
  pool-finalisation time. Out-of-band card; not added to draft chaff. Document as a "this shouldn't normally happen"
  failsafe.

### 2.6 Definition of done (Phase 2)

- [ ] After draft ends, both players see a "Pick your commander" overlay with their eligible commanders.
- [ ] Selecting routes to `DECK_BUILDING` with `Deck.commander` populated.
- [ ] Deckbuilder shows the chosen commander in its own group with the gold border (existing commander-format UI).
- [ ] Color identity validation kicks in live as cards are added (already works once `commander` is set).
- [ ] Piper fallback fires when a pool has zero eligible commanders (test: synthetic empty-legendary set).

---

## Phase 3 — Engine integration

Wire the actual game to run as Commander. **Blocked on [commander-format.md](commander-format.md) Phase 1.**

### 3.1 Format mapping at match start

- `TournamentMatchHandler.kt` (line ~417 where `BoosterGenerator.distributeBasicLandVariants` is currently called)
  — when launching a `BRAWL_DRAFT` match, construct `Format.Commander(deckSize = 60, startingLife = 25,
  commanderDamageThreshold = 16, alwaysDivertToCommand = true)` and pass it into `GameInitializer.GameConfig`.
- Each `PlayerConfig` carries its `commanderCardName` from the lobby's `LobbyPlayerState.commander`.
- `GameInitializer` (per commander Phase 1.2) routes the commander into `Zone.COMMAND` and attaches
  `CommanderComponent`.

### 3.2 Brawl-flavoured Commander config

The doc proposes two preset shapes that the host can pick between in the lobby. Both are `Format.Commander`
instances with different defaults:

| Preset | Deck size | Starting life | Cmdr damage | Notes |
|---|---|---|---|---|
| **Brawl Draft** (default) | 60 | 25 | 16 | Mirrors paper Brawl life total; faster 1v1 games |
| **Commander Draft** | 60 | 30 | 21 | Closer to Commander Legends template; slower, more recursive |

Both use `alwaysDivertToCommand = true` for Phase 1 simplicity; Phase 1.5 of the commander work upgrades this to a
player-choice modal universally.

Lobby UI shows a radio toggle between these two presets. Stored on `TournamentLobby` as `brawlPreset: BrawlPreset =
BrawlPreset.BRAWL`.

### 3.3 Deck validator changes

The existing `DeckValidator.validate(Deck, format)` enforces:
- Deck size match (currently hard-coded `MIN_DECK_SIZE = 100` for commander shape).
- Singleton (max 1 of any non-basic).
- Color identity ⊆ commander's identity.
- Commander eligibility.

For drafted formats:
- **Drop singleton.** Add a `DeckValidator` profile flag `allowDuplicates: Boolean = false`. Brawl/Commander Draft
  set it `true`. (Limited pools wouldn't survive singleton.)
- **Configurable deck size.** Already follows `Format.Commander.deckSize`; just pass 60.
- **Color identity stays.** This is what makes commander selection meaningful — you can't splash off-pivot rares.
- **Eligibility stays.** The commander pick already filtered to eligible candidates, so this is a redundant guard
  unless someone forges the request.

New error code: `DUPLICATE_NON_BASIC` (only fires when `allowDuplicates = false`); existing `NOT_SINGLETON` becomes
its alias for back-compat.

### 3.4 Definition of done (Phase 3)

- [ ] Brawl Draft lobby → 1v1 match starts with both players at 25 life, commander in command zone.
- [ ] Casting commander from command zone works; tax escalates correctly on recast.
- [ ] 16 cumulative commander damage from a single commander wins the game.
- [ ] Deck validator accepts 60-card decks with duplicates, rejects off-identity, rejects unrelated commanders.
- [ ] One full e2e Playwright scenario: lobby create → 2-player draft → commander pick → deck build → match → win
      by commander damage.

---

## Phase 4 — UI polish

### 4.1 Lobby

- Format dropdown gains "Brawl Draft" entry alongside Sealed / Draft / Winston / Grid.
- Brawl-specific lobby panel: pack count slider (3–8), preset toggle (Brawl / Commander), set chips with chaos-mix
  indicator, "guarantee legendary per pack" checkbox (default on, advanced-options hidden by default).
- Predicted pool size display: `packsPerPlayer × 15 = N cards drafted, build a 60-card deck`.

### 4.2 Commander pick overlay

- New component `web-client/src/components/draft/CommanderPickOverlay.tsx` (mirrors the existing draft overlays).
- Card grid of eligible commanders from the player's pool. Hover preview shows oracle text. Click → select. Confirm
  button locks the pick and waits for the opponent.
- Empty-pool fallback: shows the Piper card with a "No eligible commanders — using The Prismatic Piper" banner.

### 4.3 Deckbuilder

Already commander-aware (gold-border crown, commander group, identity filter). No new work; verify it picks up the
lobby-supplied commander on entry.

### 4.4 In-game

Reuses commander-format Phase 1.8 UI: command-zone widget, commander damage tally, tax badge. The damage threshold
display reads from `Format.Commander.commanderDamageThreshold` (16 for Brawl preset, 21 for Commander preset).

---

## Out of scope

- **Multiplayer Brawl Draft** (3-4 player free-for-all). Out of scope until commander multiplayer (commander-format
  Phase 3) lands.
- **Partner / Background pairs.** Defer until commander Phase 4 anyway, and they're a complication in limited (which
  legendaries pair?). Brawl Draft v1 is single-commander only.
- **Cube draft support.** A curated cube file (not booster-generated) is a separate feature.
- **Sealed Brawl.** Trivially reachable from Phase 1's infra (sealed pool + commander pick + 60-card deckbuild), but
  not designed here; treat as a follow-up that reuses everything except the draft state.
- **Auto-partner upgrade.** Commander Masters auto-grants partner to all mono-color legendaries during draft so you
  can splash colors late. Compelling but conflicts with the simpler "color identity locks the pool" mental model.
  Park as an explicit follow-up after Brawl Draft v1 has play data.

---

## Risks and unknowns

- **Engine commander runtime is the gating dependency.** Phase 1 + 2 can ship as a draft-and-pool experience without
  it (treats output as a regular sealed pool), but the format isn't really Brawl Draft until Phase 3 wires the
  command zone. Risk: shipping Phase 1+2 in isolation might confuse users about what they're playing.
- **Booster strategy override surface.** `BoosterGenerator` currently bakes `BoosterStrategy` into `SetConfig`. The
  per-call override (1.3 Option A) is small but touches `BoosterDraftHandler`, the sealed-pool path, and the
  per-set distribution code. Audit all `generateBooster` / `generateSealedPool` call sites before plumbing the new
  overload.
- **Empty legendary pool.** The Piper fallback is the right answer but requires either a real card def in
  `mtg-sets` or a synthetic placeholder. Confirm Piper exists before relying on it; otherwise add the synthetic
  path in 2.5.
- **Color identity in drafted pool.** Drafting 5 colors then committing to 2 wastes ~60% of the pool. Players will
  feel this. Two mitigations to consider but not commit to: (a) show drafted-card identity coverage during the
  commander pick, (b) allow swapping commander once during deckbuilding. Both add UI surface; defer until play
  data justifies them.
- **Cmdr-damage threshold of 16 is opinionated.** No precedent in paper; paper Brawl doesn't have commander
  damage. The 16 number is a guess at "21 scaled to 25 life." If this feels off in playtests, drop it to 21 (paper
  parity) or remove the loss condition entirely for the Brawl preset.
- **Test coverage.** Booster generation + draft state machine have decent unit tests already; commander pick and
  Brawl-shaped validator need new ones. One e2e Playwright scenario per phase is the minimum.

---

## Research basis

Wizards has shipped two reference templates for "drafted singleton-ish 1v1":

- **Commander Legends** (2020): 3 boosters × 20 cards = 60 drafted, pick 2 at a time, 60-card decks, **singleton
  dropped**, 2 legendaries per pack guaranteed, Prismatic Piper as fallback commander.
- **Commander Masters** (2023): same shape + auto-partner for ≤1-color legendaries so drafters aren't locked into
  their P1P1 commander.

Wizards has **never** shipped an official Brawl draft format; all discussion is homebrew. The community consensus
(MTG Salvation "One Vs. One drafting" and "Brawl Sealed" threads) lands in the same place: shrink the deck, boost
the pack count modestly, force a legendary, drop singleton.

This doc takes the Commander Legends shape and tunes it for paper Brawl's life total (25 instead of 40) and a
slightly higher pack count (5 instead of 3) because we're using standard 15-card packs, not Commander Legends'
20-card variants.
