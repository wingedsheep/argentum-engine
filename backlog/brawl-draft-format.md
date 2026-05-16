# Brawl / Commander Draft & Sealed Format

A drafted (or sealed) singleton-flavoured 1v1 limited format using **Commander Legends 2020 -shaped boosters**
(20-card packs with 2 dedicated legendary slots). Players build a pool, pick one legendary from that pool as their
commander, and build a 60-card deck inside the commander's colour identity. Game-time uses the `Format.Commander`
engine config with Brawl-tuned values (25 life, 16 commander damage).

The first ship target is **Brawl** (60-card decks, 25 life). A taller "Commander" preset (still 60-card decks but
30 life / 21 cmdr damage) ships at the same time as a lobby toggle — same code path, different `Format.Commander`
values.

Both **Draft** and **Sealed** ship in v1. They share booster generation, commander selection, deckbuilder, and
engine wiring; only the lobby state machine differs.

This is the singleton-flavoured 1v1 sibling of the existing Sealed / Booster Draft / Winston / Grid lobby formats.
It is intentionally **1v1 only** for the foreseeable future — multiplayer commander is its own (much larger)
project ([commander-format.md](commander-format.md) Phase 3).

## Status (2026-05-16)

Greenfield. Most prerequisites are in place:

- ✅ `BoosterGenerator.GuaranteedLegendaryBooster` strategy (`rules-engine/.../limited/BoosterGenerator.kt` →
  `mtg-sdk/.../limited/BoosterStrategy.kt`). Existing 1-legendary slot strategy is the conceptual ancestor of the
  new CMR-shaped strategy; we are adding a new sibling that ships 2 leg slots in a 20-card pack.
- ✅ Multi-set sealed pool generation with per-set distribution (`BoosterGenerator.generateSealedPool` and the
  `boosterDistribution: Map<String, Int>` overload). Each booster comes from a single set; no mid-pack mixing.
- ✅ Per-call booster-strategy override is the only `BoosterGenerator` change needed — preserves `SetConfig`
  immutability and lets the lobby decide pack shape independent of each set's default.
- ✅ `Format.Commander` engine config (`mtg-sdk/.../core/Format.kt`) — already parameterised on deck size, starting
  life, commander damage threshold, etc. Brawl values are just a config instance, not new code.
- ✅ `DeckFormat.BRAWL` / `STANDARD_BRAWL` enum entries + `isCommanderShape` helper
  (`mtg-sdk/.../core/DeckFormat.kt`).
- ✅ `TournamentLobby` supports the DRAFT and SEALED state machines (`WAITING_FOR_PLAYERS → DRAFTING|SEALED →
  DECK_BUILDING → TOURNAMENT_ACTIVE`); a `deckFormat` field already exists on the lobby.
- ✅ Commander deck validator (`DeckValidator.validate(Deck, format)` with the `Deck.commander` field).

**Blockers**

None at the SDK / engine layer. The commander runtime is already wired:
`CommanderComponent` + `CommanderRegistryComponent`, `GameInitializer` routes commanders into `Zone.COMMAND`,
`CostCalculator.commanderTax` (CR 903.8) escalates per cast-from-command, `CombatDamageManager.accumulateCommanderDamage`
records cumulative damage, and `CommanderDamageLossCheck` SBA enforces the loss threshold. Drafting and game-time
are independent workstreams.

## Locked design decisions

Research basis: Commander Legends 2020 + Commander Masters 2023 draft templates (see the closing "Research"
section). Confirmed with the host on 2026-05-16.

| Decision | Value | Reasoning |
|---|---|---|
| Pack shape | **CMR-2020: 20 cards** — 13C / 3U / 2 legendary / 1 non-leg R/M / 1 bonus | Direct copy of the paper format |
| Bonus slot | Any rarity, weighted like CMR's foil slot (mostly C, sometimes R/M) | We don't model foils as gameplay-relevant; bonus card replaces the foil slot. Keeps the 20-card count. |
| Pack synthesis | Apply CMR shape to **any** chosen set, synthesising legendary slots from whatever legendaries the set has | Lets players draft/seal with current sets (BLB, MOM, DSK, …), not just CMR/CMM. Sparse-legendary fallback documented below. |
| Packs per player | Draft default **3** (60 cards drafted), Sealed default **6** (120 cards). Host-configurable. | Draft mirrors CMR 2020; Sealed bumps past CMR's 4-pack default so 60-card decks have real depth |
| Pick size | Default **2**. Host-configurable (1 / 2 / 3). | Pick-2 is the CMR default; speeds the draft |
| Deck size | **60 minimum**, host-configurable | Brawl shape; 1v1 doesn't need 99 cards |
| Singleton | **OFF by default**. Host toggle. | A drafted pool can't sustain singleton without a much larger pack count |
| Commander pick | **After full draft / sealed pool reveal**, not at P1P1 | Lets players see the full pool before committing to colours (CMM precedent) |
| Sparse legendary fallback | The Prismatic Piper if available; synthetic vanilla 1G 2/3 Human otherwise | CMR precedent |
| Commander preset | Brawl (60 deck / 25 life / 16 cmdr damage) **or** Commander (60 deck / 30 life / 21 cmdr damage) | Host toggle. Both run under `Format.Commander`, just with different config values. |
| Player count | **1v1 only** | Multiplayer commander is a separate project |

---

## Phase 1 — `CommanderDraftBooster` strategy

Smallest shippable slice: a new `BoosterStrategy` that produces 20-card CMR-shaped packs from any registered set's
card pool. No lobby changes yet; covered by unit tests on `BoosterGenerator`.

### 1.1 New strategy

New file: `mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/limited/CommanderDraftBooster.kt`

```kotlin
data class CommanderDraftBooster(
    val commonCount: Int = 13,
    val uncommonCount: Int = 3,
    val legendaryCount: Int = 2,
    val nonLegendaryRareOrMythicCount: Int = 1,
    val bonusCount: Int = 1,
    val piperCardId: CardId? = null, // null = synthesise vanilla if needed
) : BoosterStrategy {

    override fun generate(pool: BoosterPool, random: Random): List<CardId> {
        val legendaries  = pool.byPredicate { it.isLegendary }
        val nonLegendaries = pool.byPredicate { !it.isLegendary }

        return buildList {
            addAll(pickCommons(nonLegendaries, commonCount, random))
            addAll(pickUncommons(nonLegendaries, uncommonCount, random))
            addAll(pickLegendaries(legendaries, legendaryCount, random)) // weighted 6U:3R:1M from CMR data
            addAll(pickNonLegRareOrMythic(nonLegendaries, nonLegendaryRareOrMythicCount, random)) // 7:1
            addAll(pickBonus(pool, bonusCount, random)) // weighted to mirror CMR foil distribution
        }
    }
}
```

**Sparse-legendary fallback** (`pickLegendaries`):

1. If the set has enough legendaries in the requested rarity mix, use them straight.
2. If the legendary pool is too thin, repeat from across rarities (uncommon legendaries fill rare slots, etc.).
3. If the pool has zero legendaries entirely, fill slots with `piperCardId` (if non-null) and otherwise back-fill
   with non-legendary commons of matching rarity. The pool is then flagged so the lobby can swap in Piper as the
   forced commander.

### 1.2 The Prismatic Piper

Required card definition: a fallback colourless commander. Three options:

- **Real definition in `mtg-sets`** (preferred). Single-card "Commander Legends Promos" set entry (`CMR`,
  Piper). Treat the card as a registry singleton — never appears in regular pools, only injected by
  `CommanderDraftBooster` and the post-draft commander step.
- Synthetic vanilla `1, 2/3 Human` if the real card isn't in our registry yet. Documented as a stopgap.

Either way, the booster strategy accepts an optional `piperCardId` parameter; the lobby supplies it from a central
constant (`SystemCards.PRISMATIC_PIPER`).

### 1.3 Booster generator override

Extend `BoosterGenerator` with a per-call strategy override (already proposed in the original draft of this doc as
"Phase 1 Option A"):

```kotlin
fun generateBooster(setCode: String, strategy: BoosterStrategy? = null): List<CardId> { ... }
fun generateSealedPool(setCodes: List<String>, boosterCount: Int, strategy: BoosterStrategy? = null): List<CardId>
fun generateBoosterDistribution(distribution: Map<String, Int>, strategy: BoosterStrategy? = null): List<CardId>
```

Defaults preserve current behaviour (uses the set's own `boosterStrategy`). Passing a strategy overrides for that
call. All Brawl Draft / Sealed code paths supply `CommanderDraftBooster(piperCardId = SystemCards.PRISMATIC_PIPER)`.

### 1.4 Tests

Unit tests in `BoosterGeneratorTest`:

- Pack is exactly 20 cards.
- Pack contains exactly 2 legendaries from the set (or Piper substitutes when sparse).
- 13 commons / 3 uncommons / 1 non-leg R-or-M / 1 bonus.
- Sparse-legendary fallback: synthetic set with 0 legendaries → Piper appears in legendary slots, pool is flagged.
- Across 1000 packs, rarity distributions sit within ±5% of the CMR template.

### 1.5 Definition of done (Phase 1)

- [ ] `CommanderDraftBooster` exists and is unit-tested.
- [ ] `BoosterGenerator.generateBooster(setCode, strategy = …)` overload exists; all existing call sites still
      pass the legacy strategy by default.
- [ ] `SystemCards.PRISMATIC_PIPER` resolves to a real or synthetic card definition.

---

## Phase 2 — Lobby + Draft & Sealed wiring

A new `TournamentFormat` entry, lobby-level config, and the choice between drafted and sealed pool generation.

### 2.1 New `TournamentFormat` entries

`game-server/.../lobby/TournamentLobby.kt` — add two entries between `GRID_DRAFT` and `PREMADE_DECKS`:

```kotlin
enum class TournamentFormat {
    BOOSTER_DRAFT, WINSTON_DRAFT, GRID_DRAFT,
    COMMANDER_DRAFT,    // NEW — pick-2 CMR-shaped draft, then commander pick
    COMMANDER_SEALED,   // NEW — sealed pool of CMR-shaped packs, then commander pick
    PREMADE_DECKS,
    SEALED,
}
```

Wire-protocol DTOs (`ServerMessage`, `ClientMessage`, `web-client/src/types/messages.ts`) all mirror the new enum
values.

### 2.2 Lobby-level config

`TournamentLobby` already carries `boosterCount` (Draft: packs/player; Sealed: boosters in pool) and
`picksPerRound: Int = 1` (drafted pick batch size). **Reuse both** — no parallel `packsPerPlayer` / `pickSize`
fields. Phase 2 adds only three new knobs:

```kotlin
var deckSizeMin: Int = 60,                                // Brawl shape, host-configurable 40..100
var allowDuplicates: Boolean = true,                      // singleton OFF by default
var commanderPreset: CommanderPreset = CommanderPreset.BRAWL, // BRAWL (25/16) or COMMANDER (30/21)
```

Format-default `boosterCount` is set in `LobbyHandler.handleCreateTournamentLobby` and
`handleUpdateLobbySettings` when the format switches: 3 for `COMMANDER_DRAFT`, 4 for `COMMANDER_SEALED`.
`picksPerRound` defaults to its existing 1; the host bumps it to 2 via `UpdateLobbySettings` — the doc's
recommendation of "pick 2 by default for Commander Draft" should be applied at the lobby-create defaults
later (deferred to Phase 5 UI so the host explicitly opts in).

`CommanderPreset` is a small enum on the SDK side (`mtg-sdk/.../core/Format.kt`) carrying `(deckSize,
startingLife, commanderDamage)` and a `toFormat()` builder that yields a `Format.Commander` instance for
Phase 4 to feed into `GameInitializer.GameConfig`.

### 2.3 Draft path (`COMMANDER_DRAFT`) — pick-N support

**Already wired.** `BoosterDraftHandler.handleMakePick` consumes `ClientMessage.MakePick(cardNames:
List<String>)` (plural). `TournamentLobby.makePick(playerId, cardNames)` validates
`cardNames.size == picksPerRound`, applies all picks atomically, and passes the remainder. Pick-2 (or any N)
"just works" by setting `picksPerRound` on the lobby. No new code in Phase 2.

### 2.4 Sealed path (`COMMANDER_SEALED`)

Reuse the existing `SealedSession` infrastructure:

- Lobby generates `packsPerPlayer × N` boosters via `BoosterGenerator.generateSealedPool(setCodes, boosterCount,
  strategy = CommanderDraftBooster(...))` at lobby start.
- Pool is presented to the player face-up (existing sealed UI).
- Lobby state transitions: `WAITING_FOR_PLAYERS → SEALED → COMMANDER_SELECTION → DECK_BUILDING →
  TOURNAMENT_ACTIVE`. Skips `DRAFTING`.

### 2.5 Sparse-legendary pool propagation

When `CommanderDraftBooster` flags a pool as "no legendaries", the lobby stores that flag on
`LobbyPlayerState.forcedPiperCommander = true`. Phase 3's commander pick step honours it (auto-selects Piper, no
prompt).

### 2.6 Definition of done (Phase 2)

- [x] `TournamentFormat.COMMANDER_DRAFT` and `COMMANDER_SEALED` enum entries with `isCommanderFormat` helper.
- [x] `TournamentLobby` fields: `deckSizeMin`, `allowDuplicates`, `commanderPreset`. (`boosterCount` and
      `picksPerRound` reused for "packs per player" and "pick batch size".)
- [x] `LobbySettings` DTO + `UpdateLobbySettings` client message extended; `buildLobbyUpdate` populates the
      new fields.
- [x] `LobbyHandler.handleCreateTournamentLobby` defaults `boosterCount` to 3 (Draft) / 4 (Sealed) for the
      new formats; `handleUpdateLobbySettings` applies `deckSizeMin` / `allowDuplicates` / `commanderPreset`
      changes (silently ignored on non-commander formats).
- [x] `startDeckBuilding()` accepts `COMMANDER_SEALED` and threads `CommanderDraftBooster` through every
      `generateSealedPool` overload.
- [x] `startDraft()` accepts `COMMANDER_DRAFT`; `distributeNewPacks()` uses `CommanderDraftBooster`.
- [x] TS protocol mirror in `web-client/src/types/messages.ts` — new format strings, `CommanderPreset`
      union, three new `LobbySettings` fields, three new optional fields on `UpdateLobbySettingsMessage`.
- [ ] Both paths transition to `COMMANDER_SELECTION` at the end. **(Deferred to Phase 3 — Phase 2 routes to
      `DECK_BUILDING` directly; the commander pick step is inserted before deckbuilding in Phase 3.)**
- [ ] One Kotest integration test exercising the lobby end-to-end (lobby create →
      `startDeckBuilding`/`startDraft` → pool inspection). **(Deferred — `game-server` has no unit-test
      harness today per `game-server/CLAUDE.md`. Will be covered by the Phase 4 e2e scenario instead.)**

---

## Phase 3 — Commander pick inside the deckbuilder

**Design pivot (2026-05-16):** the commander pick is folded into the existing deckbuilder rather than getting its
own lobby state + selection overlay. Rationale: the player needs to see their full pool to choose a commander
anyway, and the deckbuilder is already where pools are rendered. No new lobby state, no per-player broadcast of
the commander mid-build, no separate "selection started" envelope. The commander travels with the existing
deck-submit message (`Deck.commander` field, already plumbed for paper-Commander PREMADE_DECKS lobbies).

State machine stays:
```
COMMANDER_DRAFT:   WAITING → DRAFTING → DECK_BUILDING → TOURNAMENT_ACTIVE → COMPLETE
COMMANDER_SEALED:  WAITING → [pool generation] → DECK_BUILDING → TOURNAMENT_ACTIVE → COMPLETE
```

### 3.1 Deck-submit recognises drafted commander shape

`LobbyHandler.handleLobbyDeckSubmit` already accepts `commander: String?` for PREMADE_DECKS commander-shape
submissions. The `commanderShape` check is expanded to include the new formats:

```kotlin
val commanderShape = (lobby.format == TournamentFormat.PREMADE_DECKS &&
    lobby.deckFormat?.isCommanderShape == true) || lobby.format.isCommanderFormat
```

Outside commander-shape lobbies, the commander field is silently dropped (same defensive idiom used elsewhere,
so a stale commander on a saved deck can't leak into a Standard game).

### 3.2 Pool-aware deck validation

Drafted/sealed pools can't use Scryfall's commander-legality table (the pool is the legality universe). Phase 3
adds a new entry point on `DeckValidator`:

```kotlin
fun validateCommanderLimited(
    deck: Deck,
    pool: List<CardDefinition>,
    minDeckSize: Int = 60,
    allowDuplicates: Boolean = true,
): DeckValidationResult
```

Enforced rules:

- Every non-basic card must appear in `pool` with sufficient multiplicity (`NOT_IN_POOL` error). Basic lands
  bypass the check — they're supplied separately by `lobby.basicLands`.
- Deck size ≥ `minDeckSize` (`TOO_FEW_CARDS`). No exact-size requirement — players may overbuild.
- When `allowDuplicates = false`, the singleton cap kicks in (`TOO_MANY_COPIES`), with the usual "any number named
  X" oracle override (Relentless Rats, Persistent Petitioners). When true, no copy cap.
- Commander required (`MISSING_COMMANDER`), legal under `CommanderEligibility.isLegalCommander`
  (`INVALID_COMMANDER`), and every other card's colour identity ⊆ commander's identity
  (`COLOR_IDENTITY_VIOLATION`).

The pre-existing private helper `validateCommanderRules` is refactored to accept a `formatLabel: String` (was
`format: DeckFormat`) so the limited path can reuse it without inventing a synthetic `DeckFormat` entry. Error
codes are unchanged.

### 3.3 Deckbuilder UI (deferred to Phase 5)

The deckbuilder already renders a commander group with a gold border when `Deck.commander` is set, and
colour-identity validation lights up live in the existing commander UI. The remaining UI surface for
Phase-3-shape lobbies is:

- A "Pick your commander" affordance in the deckbuilder header for `COMMANDER_DRAFT` / `COMMANDER_SEALED` lobbies,
  listing the pool's eligible commanders (legendary creatures + override-text planeswalkers / artifacts). Default
  state: no commander chosen; submit is gated until one is.
- An explanatory chip on the lobby summary panel: "Brawl Draft — 60-card minimum, duplicates allowed".
- Sparse-pool empty-state copy: "No eligible commanders in your pool" with the same submit-disabled state. Piper
  fallback is deferred until a Piper `CardDefinition` is registered (a separate `mtg-sets` workstream is
  currently adding Commander Legends content).

### 3.4 Definition of done (Phase 3)

- [x] `commanderShape` recognises `COMMANDER_DRAFT` and `COMMANDER_SEALED` in `handleLobbyDeckSubmit`.
- [x] `DeckValidator.validateCommanderLimited` exists and enforces pool-membership, min-size, singleton toggle,
      eligibility, and colour-identity.
- [x] `validateCommanderRules` accepts a label string so the limited path can reuse it.
- [x] Limited commander submissions fail loudly when the commander is missing, ineligible, off-identity, or the
      mainboard is too small.
- [ ] Deckbuilder UI surfaces the commander picker for these lobbies. **(Deferred to Phase 5 UI polish.)**
- [ ] Sparse-pool empty state shows "No eligible commanders" without crashing. **(Phase 5; backend already
      returns `MISSING_COMMANDER` so the existing error path is the temporary surface.)**

---

## Phase 4 — Engine integration

Wire the actual game to run as Commander. The commander runtime is already wired (see Status above); this phase
just plumbs the lobby's chosen preset / commander into `GameInitializer.GameConfig`.

**Implemented at `TournamentMatchHandler.startSingleMatch`:** the `isCommanderShape` predicate is expanded to
include `COMMANDER_DRAFT` and `COMMANDER_SEALED`, and the `engineFormat` for those lobbies is built from
`lobby.commanderPreset.toFormat().copy(deckSize = lobby.deckSizeMin)`. Paper `PREMADE_DECKS` commander lobbies
keep the engine's classic defaults (100/40/21). Per-player `commanderCardName` is already plumbed through
`gameSession.addPlayer(..., commanderCardName = commander1)` and the existing commander-shape strip-from-deck
logic already runs.

### 4.1 Format mapping at match start

- `TournamentMatchHandler` — when launching a `COMMANDER_DRAFT` or `COMMANDER_SEALED` match, construct
  `Format.Commander(deckSize = lobby.deckSizeMin, startingLife = preset.startingLife, commanderDamageThreshold =
  preset.commanderDamage, alwaysDivertToCommand = true)` and pass it into `GameInitializer.GameConfig`.
- Each `PlayerConfig` carries its `commanderCardName` from the lobby's `LobbyPlayerState.commander`.
- `GameInitializer` (per commander Phase 1.2) routes the commander into `Zone.COMMAND` and attaches
  `CommanderComponent`.

### 4.2 Preset selection

`commanderPreset` from Phase 2.2 maps directly to `Format.Commander` values via the `CommanderPreset` enum. Both
presets use `alwaysDivertToCommand = true` for Phase 1 simplicity; Phase 1.5 of the commander work upgrades this
to a per-player-choice modal universally.

### 4.3 Deck validator changes

The existing `DeckValidator.validate(Deck, format)` enforces:

- Deck size match.
- Singleton (max 1 of any non-basic).
- Colour identity ⊆ commander's identity.
- Commander eligibility.

For drafted/sealed Commander formats:

- **Honour `allowDuplicates`.** Add a profile flag to `DeckValidator`. Brawl/Commander Draft+Sealed read it from
  the lobby setting (default true → singleton dropped).
- **Honour `deckSizeMin`.** Replace the hard-coded 100-card minimum with `format.deckSize`, plumbed from the
  lobby's `deckSizeMin`.
- **Colour identity stays.** This is what makes commander selection meaningful.
- **Eligibility stays.** The commander pick already filtered to eligible candidates; this is a redundant guard
  unless someone forges the request.

New error code: `DUPLICATE_NON_BASIC` (only fires when `allowDuplicates = false`); existing `NOT_SINGLETON` becomes
its alias for back-compat.

### 4.4 Definition of done (Phase 4)

- [x] `isCommanderShape` in `TournamentMatchHandler.startSingleMatch` recognises `COMMANDER_DRAFT` /
      `COMMANDER_SEALED`.
- [x] Limited commander formats build `engineFormat` from `lobby.commanderPreset.toFormat()` with the lobby's
      `deckSizeMin`.
- [x] `commanderCardName` for each player is read from `LobbyPlayerState.commander` and passed into the engine
      (no new code — the existing PREMADE_DECKS commander wiring covers it once `isCommanderShape` flips true).
- [x] Existing engine guards apply: missing commander aborts the match start with a logged warning.
- [x] Deck validator accepts 60-card decks with duplicates when `allowDuplicates = true`, rejects when false
      (covered by Phase 3's `validateCommanderLimited`).
- [ ] One full e2e Playwright scenario: lobby create → 2-player draft → commander pick → deck build → match →
      win by commander damage. **(Phase 5 — needs the deckbuilder commander picker UI before this is runnable.)**
- [ ] Sealed counterpart e2e (no draft step). **(Phase 5.)**

---

## Phase 5 — UI polish

### 5.1 Lobby

Format selector (`web-client/src/components/ui/GameUI.tsx:805–840`) gains two top-level buttons alongside Sealed /
Draft / Premade:

- **Commander Draft** — disabled when player count > 2 (1v1 only).
- **Commander Sealed** — disabled when player count > 2.

When either is active, the existing booster / pick / timer knobs continue to work (`isAnyDraft` /
`(isSealed || isWinston || isCommanderSealed)` now include the new formats); the lobby title chip reads "Commander
Draft" / "Commander Sealed" and the subtitle appends the preset tag (e.g. "3 packs · 45s per pick · Brawl 25 life").

New conditional knob panel (visible only when `isAnyCommander`):

| Control | Default | Implementation |
|---|---|---|
| Preset | Brawl (25/16) | Two-button toggle; `updateLobbySettings({ commanderPreset: 'BRAWL' \| 'COMMANDER' })` |
| Min deck size | 60 | Dropdown 40 / 50 / 60 / 75 / 100; `updateLobbySettings({ deckSizeMin })` |
| Singleton | OFF (Duplicates OK) | Two-button toggle; `updateLobbySettings({ allowDuplicates })` |

### 5.2 Pick overlay (Draft, pick-N)

Already supported by the existing draft handler — `picksPerRound: 2` makes the booster-draft pick UI accept two
cards before passing. No new component needed; the Commander Draft format inherits the existing `DraftPickOverlay`
behaviour. Host configures pick size via the same `Cards per pick` toggle exposed for paper Draft.

### 5.3 Commander pick — folded into the deckbuilder

Per the Phase 3 design pivot, the commander pick lives inside the deckbuilder rather than as a separate overlay.
`web-client/src/components/sealed/DeckBuilderOverlay.tsx` gains an inline `CommanderPickerControl`:

- Renders only when `lobbyFormat ∈ {COMMANDER_DRAFT, COMMANDER_SEALED}`.
- Filters the pool for eligible commanders (legendary creatures + planeswalkers + cards with "can be your
  commander" override text). When the pool has no eligibles, the picker shows a red "No eligible commanders" chip.
- Selecting a commander calls `setCommander(cardName)` on the draft slice; clearing is supported.
- The chosen commander travels with the existing `submitSealedDeck` message via a new
  `DeckBuildingState.commander` field. The slice merges it into the wire `deckList` (the server's
  `stripCommanderFromCards` subtracts it before building `Deck.cards`).

The submit button stays disabled until a commander is chosen *and* `totalCount >= lobby.deckSizeMin`; the counter
chip displays the dynamic minimum (`X / 60` for Brawl, `X / 100` if the host bumped the slider).

### 5.4 Deckbuilder cleanup

The existing commander group / gold-border crown / colour-identity filter (paper Commander UI) lights up
automatically once `state.commander` is set. No additional component changes were needed.

### 5.5 In-game

Reuses the existing Commander runtime UI: command-zone widget, commander damage tally, tax badge. The damage
threshold reads from `Format.Commander.commanderDamageThreshold` (16 Brawl, 21 Commander).

### 5.6 Definition of done (Phase 5)

- [x] Lobby format buttons added; commander knob panel renders when `isAnyCommander`.
- [x] Lobby title + subtitle reflect the commander format and preset.
- [x] Pick-2 toggle works for Commander Draft (reuses paper Draft's `picksPerRound`).
- [x] Deckbuilder shows a pool-filtered commander picker; submit gates on commander + min deck size.
- [x] `setCommander` action + `DeckBuildingState.commander` plumb the chosen commander through
      `submitSealedDeck` to the server.
- [ ] Playwright e2e: lobby create → draft → commander pick in deckbuilder → match → win by commander damage.
- [ ] Playwright e2e: sealed counterpart (no draft step).

---

## Out of scope

- **99-card Commander pool variant.** Reachable later by changing the deck-size minimum to 99 and packs/player to
  ~6; not in v1 because pack maths gets uncomfortable at 99 unless we use larger packs.
- **Multiplayer (3-4 players).** Out of scope until commander multiplayer (commander-format Phase 3) lands.
- **Partner / Background pairs.** Defer until commander Phase 4; complicates eligibility filtering for limited.
- **Cube draft support.** Curated cube files are a separate feature.
- **Auto-partner upgrade** (Commander Masters' mono-colour auto-partner). Compelling but conflicts with the
  simpler "colour identity locks the pool" mental model. Park as a follow-up after v1 play data.
- **True foil cosmetics.** Bonus slot replaces foil; isFoil flag on `Printing` is a separate cosmetic project.

---

## Risks and unknowns

- **Sparse legendary pools.** Some older sets have very few legendaries (Mirage: ~5 legendary creatures total).
  Synthesising 2-leg slots from such a pool means the same legendaries repeat heavily across packs. Acceptable for
  v1; surface a warning in the lobby when the host picks a set with `<20` legendaries.
- **Pick-2 pack-passing math.** Each player sees more cards per pack than pick-1; signals are weaker. Acceptable
  CMR-paper tradeoff. Be careful with auto-pick timeouts — 30s per pick (pick-1) should become 60s per batch
  (pick-2).
- **20-card packs × 3 = 60 cards drafted** is tight against a 60-card deck floor. Pre-Phase-4 playtesting should
  confirm the math works in practice; if it doesn't, bump default to 4 packs.
- **Booster strategy override surface.** `BoosterGenerator` currently bakes `BoosterStrategy` into `SetConfig`. The
  per-call override (Phase 1.3) is small but touches `BoosterDraftHandler`, the sealed-pool path, and the per-set
  distribution code. Audit all `generateBooster` / `generateSealedPool` call sites before plumbing the new
  overload.
- **Cmdr-damage threshold of 16 for the Brawl preset is opinionated.** No precedent in paper; paper Brawl has no
  commander damage. The 16 number is a guess at "21 scaled to 25 life." If playtests dislike it, drop to 21
  (paper-Commander parity) or remove the loss condition entirely for the Brawl preset.

---

## Research basis

Wizards has shipped two reference templates for "drafted singleton-ish 1v1":

- **Commander Legends** (2020): 3 boosters × 20 cards = 60 drafted, pick 2 at a time, 60-card decks, **singleton
  dropped**, 2 legendaries per pack guaranteed, Prismatic Piper as fallback commander. Foil slot = 1.
  Sealed = 4 packs.
- **Commander Masters** (2023): same shape + auto-partner for ≤1-colour legendaries so drafters aren't locked
  into their P1P1 commander.

Wizards has **never** shipped an official Brawl draft format; all discussion is homebrew. The community consensus
(MTG Salvation "One Vs. One drafting" and "Brawl Sealed" threads) lands in the same place: shrink the deck,
guarantee a legendary, drop singleton.

This doc copies the Commander Legends pack shape exactly (substituting "bonus card any rarity" for the foil slot)
and exposes the player-count-sensitive knobs (pack count, pick size, deck size, singleton, preset) as host
configuration so the same engine supports Brawl-style and Commander-style 1v1 variants from one codebase.
