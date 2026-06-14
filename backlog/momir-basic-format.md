# Momir Basic Format

Add the [Momir Basic](https://mtg.fandom.com/wiki/Momir) Vanguard format to the Argentum Engine.
Every player starts with the avatar **Momir Vig, Simic Visionary** in the command zone, and plays
a deck of 60 basic lands. The avatar grants one activated ability:

> `{X}{X}{X}, Discard a card:` Create a token that's a copy of a randomly chosen creature card
> with mana value X. Activate only as a sorcery and only once each turn.

This is an `add-feature` project (a format + new SDK/engine primitives), **not** `add-card`. The
avatar itself is content authored as a `CardDefinition`.

**Confirmed scope decisions**
- Full delivery: engine + scenario tests, then lobby/client playability, then AI — landed as
  separate reviewable units (see Phases).
- **Creature pool** = creatures in the **sets selected** in the lobby / quick-game set selector
  (set-scoped, fixed at match start). Grows automatically as more sets are selected/implemented.
- **Deck** = fixed 60-card all-five-basics, identical for both players. No deckbuilding.

---

## Phase 1 — Engine core + scenario tests

**Goal:** a fully testable, replay-deterministic Momir engine. No lobby/client/AI yet — Phase 1 is
proven via Kotest scenario tests using `Format.MomirBasic` set up directly. Phases 2–4 (server,
client, AI) build on this and are scoped at the bottom of this doc.

**Status (2026-06-14): ✅ DONE.** All work items below landed; `MomirBasicScenarioTest` (7 cases)
green, plus snapshot / lint / executor-coverage / full `rules-engine` suites. Notable finds vs. the
plan: `ActivateAbilityHandler` *already* handles non-battlefield `activateFromZone` generically
(owner + zone check), so item 1.6 needed **no handler change** — only the new enumerator; and
`calculateMaxAffordableX` had a latent `{X}{X}{X}` bug (it ignored `xCount`), now fixed.

### Ground truth (verified during planning — treat as starting facts, re-confirm before editing)

- Formats are a sealed interface `Format` in `mtg-sdk/.../sdk/core/Format.kt` (`Standard`,
  `Commander`). Add a variant.
- Randomness is deterministic and immutable: `GameRng` (SplitMix64) in
  `mtg-sdk/.../sdk/model/GameRng.kt`, threaded through `GameState.rng`. `pick(list)` /
  `shuffle(list)` return `(value, advancedRng)`. **`pick` is replay-safe only if the candidate
  list is in a deterministic order.**
- The server `CardRegistry` (`rules-engine/.../engine/registry/CardRegistry.kt`) is populated with
  **every catalogued set** at startup (`game-server/.../config/GameBeansConfig.kt:31`). It exposes
  `getCard` / `requireCard` / `allCardNames` / `hasCard` — but **no predicate iteration and no
  by-mana-value index**.
- Token-copy executors live in `rules-engine/.../engine/handlers/effects/token/`. The helper
  `CreateTokenCopyOfChosenPermanentExecutor.createTokenCopy(state, chosenId, controllerId,
  staticAbilityHandler)` copies the `CardComponent` off an entity **already in a zone** and places
  it as a battlefield token. **There is no path to mint a token from a bare `CardDefinition`.**
- Card instantiation from a `CardDefinition` lives in
  `GameInitializer.createCardEntity(cardDef, ownerId, printingRef)`
  (`rules-engine/.../engine/core/GameInitializer.kt:319`). Commander placement into `Zone.COMMAND`
  (≈ lines 234-277) is the template for placing the avatar.
- `{X}{X}{X}` already works: `xCount = 3` → the solver charges 3× the single chosen X
  (`ManaSolver.kt:1644`, `CastSpellEnumerator.kt:474`). The chosen per-symbol X flows into
  `EffectContext.xValue`, read by `DynamicAmount.XValue`. **So MV = X, payment = 3X, with no new
  cost math.**
- `TimingRule.SorcerySpeed` and `ActivationRestriction.OncePerTurn` exist and are enforced
  (`checkActivationRestriction`).
- **`ActivatedAbilityEnumerator` does NOT scan the command zone** — it iterates battlefield
  permanents and filters own abilities to `activateFromZone == Zone.BATTLEFIELD`.
  `GraveyardAbilityEnumerator` is the precedent for a non-battlefield-zone enumerator (scans a zone,
  filters `activateFromZone`, gates `SorcerySpeed` on `canPlaySorcerySpeed`, reuses the composite
  cost path incl. discard + X surfacing).
- Effect executors receive `CardRegistry` by constructor injection (`TokenExecutors.executors()`).

### Work items

- [x] **1.1 — `Format.MomirBasic` SDK variant.** In `mtg-sdk/.../sdk/core/Format.kt`:
  ```kotlin
  @Serializable
  data class MomirBasic(
      val startingLife: Int = 20,
      val startingHandSize: Int = 7,
      val avatarCardName: String = "Momir Vig, Simic Visionary",
      /** Creature card names from lobby-selected sets, PRE-SORTED for deterministic rng.pick. */
      val eligibleCreatureNames: List<String> = emptyList(),
  ) : Format
  ```
  `eligibleCreatureNames` is the **set-scope seam**: the server (Phase 2) computes creature names
  from the selected sets, sorts them, and passes them in. The engine treats the list as opaque,
  pre-sorted, deterministic data — keeping the engine a pure function of `GameState`.

- [x] **1.2 — `CreateRandomCreatureTokenWithManaValueEffect` SDK type.** Add to
  `mtg-sdk/.../sdk/scripting/effects/TokenEffects.kt` (`@Serializable`, `@SerialName`,
  `description`) with field `manaValue: DynamicAmount`. Add the `Effects.*` facade. **Update
  `docs/card-sdk-language-reference.md` in the same change** (load-bearing rule). If the effect
  reads/writes a named pipeline variable, classify it in `CardLinter.dataflowFields`.

- [x] **1.3 — Mint-token-from-`CardDefinition` helper.** New `TokenFromDefinition.kt` in the token
  package. Extract the `CardComponent`-building block out of `GameInitializer.createCardEntity` into
  a shared function used by both call sites, then reuse `createTokenCopy`'s battlefield tail
  (`TokenComponent`, `ControllerComponent`, `SummoningSicknessComponent`, static-ability components,
  `BattlefieldEntry.place`, `applyGlobalEntersWithCounters`, `ZoneChangeEvent(fromZone = null)`).
  **Must route through the normal ETB/replacement pipeline** so the copied creature's own
  enters-with-counters (graft / modular / Hangarback-style) and ETB triggers fire correctly — not a
  bare zone insert.

- [x] **1.4 — `CreateRandomCreatureTokenWithManaValueExecutor`.** New executor
  `(cardRegistry, staticAbilityHandler, amountEvaluator)`, registered in
  `TokenExecutors.executors()`. Internally sequences three atomic steps (gather → select → mint):
  1. `mv = evaluate(effect.manaValue, context)` (= chosen X).
  2. Read `eligibleCreatureNames` from `state.format as MomirBasic`; **filter (don't re-collect)** by
     `cardRegistry.requireCard(name).let { it.isCreature && it.cmc == mv }`, preserving the pre-sorted
     order.
  3. If the filtered list is empty → no-op success (cost already paid, CR: nothing happens). Else
     `val (name, rng2) = state.rng.pick(filtered)`, thread `state.copy(rng = rng2)`, delegate to
     `TokenFromDefinition.mint(...)`.

- [x] **1.5 — `CommandZoneAbilityEnumerator`.** New enumerator modeled line-for-line on
  `GraveyardAbilityEnumerator`: scan `ZoneKey(playerId, Zone.COMMAND)`, resolve each entity's
  `CardDefinition` via `context.cardRegistry`, take abilities with
  `activateFromZone == Zone.COMMAND`, gate `TimingRule.SorcerySpeed` on `canPlaySorcerySpeed`, run
  `checkActivationRestriction` (`OncePerTurn`), reuse the composite-cost path (mana `{X}{X}{X}` +
  discard) including `abilityHasXCost` / `maxAffordableX`. Register in `LegalActionEnumerator`
  alongside `GraveyardAbilityEnumerator`.

- [x] **1.6 — `ActivateAbilityHandler` command-zone support (HIGHEST RISK — read first).**
  Confirm/extend the handler to resolve a command-zone-sourced ability from the entity's
  `CardDefinition`, build `EffectContext` with a non-battlefield source, and thread the chosen X
  through the `ActivateAbilityChooseX` continuation. The enumerator gap is confirmed and easy to
  fill; if the handler assumes a battlefield source, activation fails even with a correct
  enumerator. **Read this file before committing to the rest of the phase.**

- [x] **1.7 — Avatar `CardDefinition` content.** Author **Momir Vig, Simic Visionary** as a Vanguard
  avatar in `mtg-sets` (Vanguard/promo package; confirm set code with maintainer), carrying one
  `ActivatedAbility`:
  ```
  cost   = Composite( Atom(Mana "{X}{X}{X}"), Atom(Discard count = 1) )
  effect = CreateRandomCreatureTokenWithManaValueEffect(manaValue = DynamicAmount.XValue)
  timing = TimingRule.SorcerySpeed
  restrictions = [ ActivationRestriction.OncePerTurn ]
  activateFromZone = Zone.COMMAND
  ```
  Register it in the set file. Re-bless `CardDefinitionSnapshotTest` / `CardLintTest` if the corpus
  changes.

- [x] **1.8 — GameInitializer wiring.** Add a `Format.MomirBasic` branch (mirror the commander block
  ≈ lines 157-277): set starting life (20) in `formatStartingLife`, instantiate the avatar via
  `createCardEntity` (resolved from `avatarCardName`), attach a marker component (reuse
  `CommanderComponent` or add a tiny `VanguardAvatarComponent`), and place it into
  `ZoneKey(playerId, Zone.COMMAND)` per player. The 60-basic deck flows through the existing library
  path unchanged.

- [x] **1.9 — Scenario tests (the real gate).** Kotest in `rules-engine` (set up `Format.MomirBasic`
  directly):
  - **Determinism / replay:** same `seed` + same `eligibleCreatureNames` ⇒ identical chosen creature.
  - **MV filter:** X = N picks a creature with `cmc == N`; **empty pool at that MV ⇒ no token, cost
    still paid.**
  - **Restrictions:** once-per-turn enforced; sorcery-speed only; discard cost paid; `{X}{X}{X}`
    charges 3X.
  - **Minted token:** summoning sick, ETB triggers fire, own enters-with-counters apply, its own X
    reads 0 (never went on the stack, so no `CastX`).

### Files (Phase 1)

| Module | File | Change |
|---|---|---|
| `mtg-sdk` | `sdk/core/Format.kt` | add `MomirBasic` variant |
| `mtg-sdk` | `sdk/scripting/effects/TokenEffects.kt` | add effect type + facade |
| `mtg-sdk` | `docs/card-sdk-language-reference.md` | document the new effect |
| `rules-engine` | `engine/handlers/effects/token/TokenFromDefinition.kt` | new mint helper |
| `rules-engine` | `engine/handlers/effects/token/CreateRandomCreatureTokenWithManaValueExecutor.kt` | new executor + register in `TokenExecutors.kt` |
| `rules-engine` | `engine/legalactions/enumerators/CommandZoneAbilityEnumerator.kt` | new + register in `LegalActionEnumerator` |
| `rules-engine` | `engine/handlers/actions/ability/ActivateAbilityHandler` | command-zone `activateFromZone` + X threading |
| `rules-engine` | `engine/core/GameInitializer.kt` | `MomirBasic` branch (life + avatar placement); extract shared `CardComponent` builder |
| `mtg-sets` | avatar set package | Momir Vig avatar `CardDefinition` + register |

### Verification (Phase 1)
- `./gradlew :rules-engine:test` for the new scenario tests; `just test-rules`.
- Re-bless `:mtg-sets` `CardDefinitionSnapshotTest` / `CardLintTest` if the avatar changes the corpus.
- `just build`.

### Top risks (Phase 1)
1. **`ActivateAbilityHandler` may assume a battlefield source** (item 1.6). Make-or-break; read first.
2. **Pool determinism.** Store pre-sorted on the format and *filter* (never re-collect from the
   registry's unspecified map order), or replays desync.
3. **Self enters-with-counters / ETB-replacement on the minted token** — must run the normal ETB
   pipeline (item 1.3), not a bare zone insert, or such tokens enter with wrong stats.

---

## Later phases (scoped, not yet started)

### Phase 2 — Server / lobby (playability)
- Add a `MomirBasic` option to the format selector (`TournamentFormat` enum + `LobbyHandler` /
  `TournamentMatchHandler` wiring; `quickGameLobbySlice` / `lobbySlice` on the client).
- At match start: build the fixed 60-basic `Deck` per seat; compute `eligibleCreatureNames` from the
  lobby's **selected sets** (reuse `activeSets()` / the set-selection mechanism), filter to creatures,
  sort, pass into `Format.MomirBasic`; set `gameSession.engineFormat`.

### Phase 3 — Client (playability)
- Render the avatar in the command-zone UI (reuse commander-zone rendering).
- Surface the X-cost + discard activation (the legal-action `hasXCost` / `maxAffordableX` already
  drives the X prompt; the discard target picker already exists). Animate the randomly-minted token
  entering.

### Phase 4 — AI
- Teach the built-in AI (`ai` module) to activate Momir at sorcery speed when it has spare mana and a
  discardable card: choose the largest affordable X (bounded by `maxAffordableX`) with a non-empty
  creature pool, preferring higher X. Heuristic only — the random pool makes value estimation coarse
  (treat the result as a generic MV-X body). Gate behind the format.
