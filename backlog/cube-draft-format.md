# Cube Draft + Cube Pool Play

Let players build their own **cube** (a hand-curated singleton card pool) and use it as the pack
source for every limited flow the lobby already supports — Booster Draft, Winston, Grid, Sealed —
plus a new no-draft **Pool Play** mode where everyone deckbuilds from the whole cube at once. Ship
one or more bundled example cubes so a host can start a cube game without curating 360 cards first.

This is an `add-feature` project (server/client capability + a new engine-side `limited` primitive),
**not** `add-card` — except the appendix, which is a plain `add-card` backlog for the 47 cards the
bundled Standard Cube is missing.

## Status: **next pick** (decided 2026-07-25)

This is the chosen next feature project, ahead of the other open backlog items.

**Why this one.** It multiplies content that already exists instead of widening the surface: 313/360
of the bundled example cube is implemented, pack generation is already a pure function of `SetConfig`,
and cube storage mirrors saved decks. One feature turns limited from "draft the sets we support" into
an indefinitely replayable format a friend group can tune themselves — and it was requested by a
player, so there's real demand behind it.

**Considered and deferred:**

- *Autonomous bug-hunting gauntlet* (invariant oracle + self-play fuzz + delta-debug shrinker over
  compact replays) — highest long-term leverage on correctness confidence, and the foundation
  [`forge-parity-harness.md`](forge-parity-harness.md) would need anyway. Deferred, not dropped: the
  strongest candidate for the pick after this one.
- *Live MCTS AI difficulty tier* (promote `gym-trainer` into the lobby) — biggest experiential upgrade
  for solo play, but doesn't help the friend-group play that Cube serves directly.
- *More format variants* ([`emperor-format.md`](emperor-format.md), [`momir-basic-format.md`](momir-basic-format.md),
  [`brawl-draft-format.md`](brawl-draft-format.md)) — explicitly lower priority. Emperor in particular
  is large engine risk (CR 801 range of influence touches targeting, attacking, and every "each
  player" effect) for a format needing six simultaneous players.

## Confirmed scope decisions

- **Cube storage:** account-owned rows in Postgres, mirroring saved decks (`decks` table →
  `AccountDeckController`), with the localStorage library as the guest/offline path (mirroring
  `deckLibrary` / `banListLibrary` and merged by a `useUnifiedCubes` hook like `useUnifiedDecks`).
- **Cube authoring:** *both* paste-a-text-list (reuse `parseArenaDeckList`) *and* an in-app cube
  editor with card search. No Cube Cobra integration (no external fetch dependency).
- **Pool Play** = **full cube, unlimited copies**: every player deckbuilds from the entire cube list
  simultaneously, no contention between players (so no live locking UX). A host checkbox on a cube
  lobby, not a separate lobby type.
- **Example cube:** the ~360-card Standard cube Vincent supplied (see appendix). 313/360 cards are
  already implemented; the missing 47 are listed at the bottom as an `add-card` checklist.

---

## Ground truth (verified 2026-07-25 — re-confirm before editing)

- **Pack generation is a pure function of set configs.** `BoosterGenerator`
  (`rules-engine/.../engine/limited/BoosterGenerator.kt`) is `class BoosterGenerator(val
  availableSets: Map<String, SetConfig>)` — no Spring, no I/O, and it imports no specific set. A
  `SetConfig` carries `cards`, `basicLands`, `printings`, `boosterStrategy`, and display metadata.
  **This is the seam:** anything that can produce a `SetConfig` can feed every existing limited flow.
- **`BoosterStrategy` is `fun interface (pool, random) -> List<CardDefinition>`**
  (`mtg-sdk/.../sdk/limited/BoosterStrategy.kt`) — stateless and called **once per pack**. Existing
  strategies (`StandardBooster`, `PlayBooster`, `GuaranteedLegendaryBooster`,
  `CommanderDraftBooster`) all pick by `Rarity` via a private `RarityPicker` that de-duplicates
  **within one pack only**. ⇒ **A cube cannot be dealt through `BoosterStrategy`**: consecutive packs
  would repeat cards, and a cube is singleton across the whole draft. This is the one genuinely new
  engine primitive the feature needs.
- **All five pack-generation call sites live in `TournamentLobby`**
  (`game-server/.../lobby/TournamentLobby.kt`): `startDeckBuilding` (2 — explicit distribution and
  even distribution), `distributeNewPacks` (booster draft, per player per round),
  `startWinstonDraft` (~line 956), `startGridDraft` (~lines 1002/1013). Every one goes through
  `boosterGenerator.generateBooster(...)` / `generateSealedPool(...)` with `setCodes`.
- **`TournamentFormat`** (same file, line 18) is the format axis: `SEALED`, `DRAFT`, `WINSTON_DRAFT`,
  `GRID_DRAFT`, `COMMANDER_DRAFT`, `COMMANDER_SEALED`, `PREMADE_DECKS`. **`LobbyGameMode`** is the
  orthogonal mode axis (`TOURNAMENT` / `FREE_FOR_ALL` / `TWO_HEADED_GIANT` / `TEAM_VS_TEAM`).
  Cube is **neither** a new format nor a new mode — it is a new *pack source*, orthogonal to both.
- **The ban list is the precedent to copy for cube plumbing.** `bannedCardNames: Set<String>` on the
  lobby, sent as the *full list every time* via `ClientMessage.UpdateLobbySettings.bannedCardNames`,
  edited client-side by `BanListEditor.tsx` (searches `/api/cards`, chips, save/load named lists via
  the localStorage `banListLibrary`). Server-authoritative, and **the lobby never touches the DB** —
  exactly the shape a cube needs, so guests and unsaved cubes work too.
- **Basic lands** come from `boosterGenerator.getBasicLands(setCodes)`, which returns the first
  selected set's `basicLands` filtered to `metadata.inBooster` and de-duped by
  `BasicLandArt.standardFirst`. A cube has no basics of its own ⇒ the cube must name a basic-land art
  source.
- **`updateSets(newSetCodes)` validates every code against `boosterGenerator.getSetConfig(code)`**
  and rebuilds `boosterDistribution`. A synthetic cube code will not resolve there — cube lobbies
  must set `setCodes`/`setNames` directly, not through `updateSets`.
- **`ServerMessage.AvailableSet` is built from the global `boosterGenerator.availableSets`** in
  `ConnectionHandler.buildAvailableSetsList()` (used twice) and again in `TournamentLobby` ~line
  1593. A cube must **not** leak into those lists (it is per-lobby, not a catalogued set).
- **Deck submission derives the sideboard as `pool − maindeck`** (`SideboardDerivation.fromPool`,
  CR 100.4b) and seeds the SIDEBOARD zone. With Pool Play the "pool" is the whole cube ⇒ a 300+ card
  sideboard would be seeded. Must be suppressed for Pool Play.
- **Sealed pool validation** (`SealedSession.validateDeck`, and `DeckValidator.validateCommanderLimited`
  for the commander shapes) enforces min 40, "card must be in pool", pool copy counts, and a hard
  4-copy cap. Pool Play needs the copy cap without the pool-count check.
- **Lobby recovery persists card *names*** (`persistence/dto/PersistentLobby.kt`:
  `cardPoolNames`, `currentPackNames`, `packQueueNames`, `winstonMainDeckNames`, …). It has **no**
  field for `bannedCardNames` today, so a lobby restart already loses the ban list; a cube lobby must
  do better (see risk 3).
- **Deck-list import already exists and already reports coverage.**
  `web-client/.../deckbuilder/parseArenaDeck.ts` parses Arena / Moxfield / plain text;
  `DeckbuilderPage.tsx` resolves against the `/api/cards` catalog and renders
  `"313 matched of 360 (47 placeholder)"` plus a per-line "Not implemented yet" list. Cube import
  should reuse both wholesale.
- **Example *decks* are a compiled-in Kotlin constant** (`DecksController.EXAMPLE_DECKS`, served at
  `GET /api/decks/examples`).
- **AI:** `SealedDeckGenerator` (`ai` module) builds decks from a pool but documents that eligible
  sets must be `SetConfig.fullyImplemented` and non-`extensionSet` — a synthetic cube config must set
  those flags coherently. Draft pick suggestion + deckbuild auto-build are gated per-lobby by
  `aiAssistEnabled`.

---

## Design — a cube is a synthetic `SetConfig` plus a stateful dealer

Two pieces, and every existing limited flow comes along for free:

1. **`SetConfig` synthesis.** Resolve the cube's card names against `CardRegistry` into
   `List<CardDefinition>`, wrap them in a `BoosterGenerator.SetConfig(setCode = "CUBE", setName =
   cube.name, cards = …, basicLands = <from the cube's basic-land art set>)`, and hand the lobby a
   **derived** generator (`boosterGenerator.withSets(mapOf("CUBE" to cubeConfig))`) whose
   `availableSets` is base + cube. The lobby then runs with `setCodes = listOf("CUBE")` and
   `setNames = listOf(cube.name)`, so `getBasicLands`, display, ban lists, and deck validation work
   unchanged.
2. **`CubeDealer`** — the new primitive. `BoosterStrategy` cannot express "deal without
   replacement across packs", so packs come from a dealer that shuffles the cube **once** and slices:

   ```kotlin
   class CubeDealer(cube: List<CardDefinition>, private val packSize: Int, seed: Long) {
       fun deal(packs: Int): List<List<CardDefinition>>   // consumes from the shuffle
       val remaining: Int
   }
   ```

   One dealer per lobby, shared by all players — so no card appears twice in the whole draft, across
   players or packs. Winston/Grid take one big slice (`deal(n).flatten()`); Sealed takes
   `packsPerPlayer` per seat; Booster Draft takes one pack per player per round.

Capacity is then an explicit host-facing constraint: `players × packsPerPlayer × packSize ≤ cube
size`. A 360-card cube seats 8 players at 3×15 exactly — which is why 360 is the community-standard
cube size.

Chaos boosters, `boosterDistribution`, and per-set pack rounds are all meaningless for a cube and
must be inert (not merely ignored) in cube mode.

---

## Phase 1 — `Cube` model, resolution, and `CubeDealer` (engine + tests)

- [x] **1.1 — `CubeList` SDK/server model.** Card **names + counts** (a cube is usually singleton but
  some run duplicates), optional pinned `PrintingRef` per card (so a cube can specify art), a
  `basicLandSetCode` for the basic-land art source, `name`, and `packSize` default. Where it lives
  depends on 1.2 — if only the server resolves cubes, `game-server/.../cube/` is the right home and
  `mtg-sdk` stays untouched.
- [x] **1.2 — `CubeResolver`.** `(CubeList, CardRegistry, PrintingRegistry) → ResolvedCube` with an
  explicit failure list for unresolvable names. **A cube with unresolved names is not playable** —
  return the misses so the host sees "47 cards in this cube aren't implemented yet" rather than
  silently drafting a 313-card cube. Applies pinned printings via `CardDefinition.withPrinting`.
- [x] **1.3 — `CubeDealer`** in `rules-engine/.../engine/limited/`. Shuffle once with a seeded
  `Random(seed)`, deal by slicing, expose `remaining`. Pure, no Spring. Unit tests: no duplicate card
  identity across all dealt packs; exact pack sizes; `deal` past capacity fails loudly with a message
  naming the shortfall; same seed ⇒ same deal.
- [x] **1.4 — `BoosterGenerator.withSets(extra)`** returning a new generator with
  `availableSets + extra`. Keeps the generator immutable and the global bean untouched. Tiny, but it
  is what makes the synthetic-`SetConfig` trick safe.
- [x] **1.5 — `CubeSetConfig.of(resolvedCube, boosterGenerator)`** building the synthetic
  `SetConfig`: `cards` from the cube, `basicLands` from `basicLandSetCode`, `sealedSupported = true`,
  `incomplete = false`, `extensionSet = false`, `variantChance = 0.0`, `boosterStrategy` =
  a strategy that is never actually consulted in cube mode (dealer path) — assert that rather than
  leaving a booby trap.

**Verification:** `just test-rules` (dealer), `just test-server` (resolver + set-config synthesis).

## Phase 2 — Lobby wiring (Draft / Winston / Grid / Sealed from a cube)

- [x] **2.1 — `TournamentLobby` cube fields.** `var cube: ResolvedCube? = null`, a lazily-built
  cube-scoped generator, `cubeDealer`, and `packSize`. `val isCube: Boolean get() = cube != null`.
- [x] **2.2 — One pack-source helper, five call sites.** Add a private
  `fun packsFor(count: Int): List<List<CardDefinition>>` that deals from `cubeDealer` when
  `isCube`, else delegates to the existing `boosterGenerator` calls. Route `startDeckBuilding`,
  `distributeNewPacks`, `startWinstonDraft`, and `startGridDraft` through it. Resist inventing a
  `PackSource` interface until there is a third source — one nullable field plus one helper covers
  both cases (see `feedback_no_singleuse_patterns`).
- [x] **2.3 — Capacity + start-gating.** Reject `startDraft` / `startDeckBuilding` on a cube lobby
  when `players × packsPerPlayer × packSize > cube.size`, with a host-readable message
  ("8 players × 3 packs × 15 = 360 cards needed, cube has 313"). Also reject a cube with unresolved
  names.
- [x] **2.4 — `UpdateLobbySettings.cubeCards` (+ `cubeName`, `packSize`, `cubeBasicLandSetCode`).**
  Full list every time, exactly like `bannedCardNames`. Server resolves and stores; clearing it
  (`[]`) returns the lobby to normal set-based play. Cube lobbies set `setCodes`/`setNames` directly
  and **must not** go through `updateSets`.
- [x] **2.5 — Inert-in-cube-mode settings.** `chaosBoosters`, `boosterDistribution`, and the set
  picker are meaningless with a cube: reject or ignore them server-side *and* hide them client-side
  (don't leave a control that silently does nothing — see `feedback_suppress_stale_ui_state`).
- [x] **2.6 — Broadcast.** Extend the lobby state message with `cubeName` / `cubeCardCount` /
  `packSize` so joiners and spectators see "Drafting: Vincent's Standard Cube (360)" instead of a
  set name. Do **not** add the cube to `AvailableSet` lists.

**Verification:** `just test-server`; then an E2E cube draft (`docs/e2e-test-patterns.md`).

## Phase 3 — Cube library, import, and editor UI

- [x] **3.1 — `cubes` table** (Flyway `V12__cubes.sql`):
  `id, user_id → users(id) ON DELETE CASCADE, name, card_count, data TEXT NOT NULL, created_at,
  updated_at` + `idx_cubes_user`. `data` is the cube JSON verbatim; `name`/`card_count` denormalized
  for list views. Straight copy of the `decks` table's shape and rationale.
- [x] **3.2 — `CubeRow` + `CubeRepository` + `AccountCubeController`** at `/api/account/cubes`,
  mirroring `AccountDeckController` (Spring Data JDBC, kotlinx.serialization, `?full` list variant,
  every operation scoped to the authenticated user). Gate on
  `@ConditionalOnProperty("accounts.enabled")` like the deck controller.
- [x] **3.3 — `cubeLibrary` (localStorage) + `useUnifiedCubes`.** Mirror `banListLibrary.ts` for the
  guest path and `useUnifiedDecks` for the local+cloud merge, so a guest can still build and use a
  cube without an account.
- [x] **3.4 — Cube import.** Reuse `parseArenaDeckList` + the deckbuilder's `resolveAgainstCatalog`,
  and reuse the existing coverage readout ("313 matched of 360 (47 placeholder)"). Cube-specific
  addition: since an unresolved cube is unplayable, offer **"Drop the 47 unimplemented cards"** and
  block "Use this cube" until the list resolves cleanly.
- [x] **3.5 — Cube editor.** `BanListEditor.tsx` is the closest existing component (catalog search,
  chips, save/load named lists) but a 360-card cube needs more: colour/type/CMC curve summary,
  section grouping, and duplicate/count handling. Prefer reusing the deckbuilder's search panel and
  `cardFilter`/`cardGrouping` over growing the ban-list editor.
- [x] **3.6 — Lobby host control.** A "Cube" panel next to the set picker: pick a cube from the
  library / paste a list, choose pack size and packs per player, and see the capacity check live
  ("360 cards — seats 8 players at 3×15").

## Phase 4 — Pool Play (no draft, unlimited copies)

- [x] **4.1 — `cubePoolPlay: Boolean` lobby flag** (shipped as a Sealed-packs / Pool-Play toggle). On `startDeckBuilding`, every
  player's `cardPool` is the **entire cube** and the lobby goes straight to `DECK_BUILDING`. No
  dealer involvement, so the capacity check in 2.3 does not apply — a 100-card cube is a perfectly
  fine Pool Play pool.
- [x] **4.2 — Validation.** Reuse the pool validator but drop the "copies available in pool" check;
  keep min 40 and the copy cap. Cap is **4 copies** (constructed-normal, `POOL_PLAY_COPY_LIMIT`);
  basics stay unlimited. A host-settable cap / singleton house rule was *not* built — nobody has asked
  for one, and a toggle with no second value behind it is the kind of setting that later has to be
  un-shipped. Add it when a group actually wants singleton Pool Play.
- [x] **4.3 — Suppress the derived sideboard.** `SideboardDerivation.fromPool(wholeCube, deck)` would
  seed a 300+ card SIDEBOARD zone. For Pool Play, submit an empty sideboard (or an explicit
  player-chosen one) — do not derive it from the pool.
- [x] **4.4 — Deckbuilder overlay at cube scale.** `DeckBuilderOverlay` renders a sealed pool
  (~90 cards, per-copy). A 360-card unlimited-copies pool needs the deckbuilder's search/filter
  ergonomics, not the sealed pool grid. Check performance and the "copies owned" affordance, which is
  meaningless here. **UX-review this flow end to end** (`feedback_ux_review`).

## Phase 5 — Bundled example cubes

- [ ] **5.1 — Ship the Standard cube** as a classpath resource
  (`game-server/src/main/resources/cubes/*.txt`, plain "1 Cardname" lines) rather than a 360-entry
  Kotlin constant, served alongside `GET /api/decks/examples` as `GET /api/cubes/examples`.
- [ ] **5.2 — A startup coverage check**, not a hard failure: log which example cubes resolve fully
  and mark unresolved ones as unavailable in the API response, so a bundled cube can't produce a
  broken lobby. Once the appendix's 47 cards land, the Standard cube resolves 360/360.
- [ ] **5.3 — Optionally add a second, smaller example cube** built entirely from long-implemented
  sets (a ~180-card "peasant"-style cube), so cube play is demoable at 2–4 players without depending
  on the newest sets. Only worth it if 5.2 shows the Standard cube is short.

## Phase 6 — AI + polish

- [ ] **6.1 — AI drafting from a cube.** The pick advisor and `SealedDeckGenerator` must work off the
  synthetic config; verify they don't assume rarity distribution or a real set code. Cube packs have
  no rarity curve, so an advisor that leans on rarity as a proxy for quality will pick badly.
- [ ] **6.2 — Replays / match history.** `format`/set columns in `match_results` should record the
  cube (name + card count), not `"CUBE"`, or cube games become indistinguishable in stats.
- [ ] **6.3 — Cube sharing.** A share link/code for a cube, mirroring the deck share link
  (`shareDeck.ts`, deflate + collector keys). 360 names will be a long URL — measure before
  committing to URL-only sharing; a short code backed by the `cubes` table may be the better answer.

---

## Files

| Module | File | Change |
|---|---|---|
| `rules-engine` | `engine/limited/CubeDealer.kt` | new — shuffle-once, deal-without-replacement |
| `rules-engine` | `engine/limited/BoosterGenerator.kt` | add `withSets(extra)` |
| `game-server` | `cube/CubeList.kt`, `cube/CubeResolver.kt`, `cube/CubeSetConfig.kt` | new |
| `game-server` | `lobby/TournamentLobby.kt` | cube fields, `packsFor` helper, capacity gate, Pool Play branch |
| `game-server` | `protocol/ClientMessage.kt` | `UpdateLobbySettings.cubeCards` + friends |
| `game-server` | `protocol/ServerMessage.kt` | cube name/size/packSize in lobby state |
| `game-server` | `handler/LobbyHandler.kt` | wire the new settings; block set-picker paths in cube mode |
| `game-server` | `deck/DeckValidator.kt` / `SealedSession` | Pool Play copy cap without pool counts |
| `game-server` | `deck/SideboardDerivation.kt` call sites | suppress for Pool Play |
| `game-server` | `persistence/Rows.kt`, `persistence/Repositories.kt` | `CubeRow` + `CubeRepository` |
| `game-server` | `controller/AccountCubeController.kt` | new — `/api/account/cubes` |
| `game-server` | `persistence/dto/PersistentLobby.kt` + `LobbyConverter.kt` | cube names + dealer remainder |
| `game-server` | `resources/db/migration/V12__cubes.sql` | new table |
| `game-server` | `resources/cubes/standard-cube.txt` | bundled example |
| `web-client` | `store/cubeLibrary.ts`, `store/useUnifiedCubes.ts` | new |
| `web-client` | `components/cube/CubeEditor.tsx` (+ import modal) | new |
| `web-client` | lobby settings UI | cube panel, Pool Play checkbox, hide set/chaos controls |
| `e2e-scenarios` | new spec | cube draft + Pool Play happy paths |

## Top risks

1. **Singleton across the whole draft.** The `BoosterStrategy` interface *looks* like the natural
   extension point and is the wrong one — a `CubeBooster : BoosterStrategy` would repeat cards
   between packs, and the bug is invisible in a 2-player 1-pack smoke test. Phase 1.3's dealer test
   ("no duplicate identity across all dealt packs") is the guard.
2. **The synthetic set code leaking.** `"CUBE"` must not reach `AvailableSet` lists, `updateSets`,
   the client set picker, or match-history set columns. Grep every `availableSets` consumer.
3. **Lobby recovery mid-cube-draft.** `PersistentLobby` stores names only and has no cube field. Add
   `cubeCardNames` *and* the dealer's remaining-cards list — without the remainder, a restart
   re-shuffles and can hand out cards already drafted.
4. **Pool Play at scale.** A 360-card unlimited pool through the sealed deckbuild overlay plus a
   pool-derived sideboard is the combination most likely to be slow or wrong; 4.3 and 4.4 exist
   specifically for it.

## Open questions

- ~~**Pool Play copy cap:** 4-of vs. singleton vs. host-set number?~~ **Resolved: 4-of, fixed** (see
  4.2). Revisit only on a real request for singleton Pool Play.
- **Cube rarity/pack shape:** some cubes want a guaranteed "bomb" slot per pack. Out of scope here —
  worth confirming nobody expects it in v1.
- **Cube visibility:** are cubes private to their owner, or should there be a public/shared cube
  browser? Phase 6.3 assumes sharing by link/code only.

---

## Appendix — Standard cube: 47 missing cards (`add-card` backlog)

The supplied 360-card Standard cube resolves 313/360 today. These 47 block Phase 5.1 from shipping a
fully-playable bundled cube. Implement with the **`add-card` skill** (it handles Scryfall lookup,
oracle errata, set registration, and the scenario test). Set codes are the *canonical earliest*
printing, per the reprint rule (`just check-card-printing "<Card>"`).

**WOE — Wilds of Eldraine (12)**
- [x] Restless Bivouac · Restless Cottage · Restless Fortress · Restless Spire · Restless Vinestalk
      *(land cycle — five of the same shape, do them as one batch)*
- [ ] Torch the Tower *(common instant)* · Royal Treatment *(uncommon instant)*
- [ ] Syr Ginger, the Meal Ender *(legendary artifact creature)*
- [ ] Bramble Familiar // Fetch Quest · Hearth Elemental // Stoke Genius ·
      Horned Loch-Whale // Lagoon Breach · Elusive Otter // Grove's Bounty
      *(all four are Adventure cards — `CardLayout.ADVENTURE`, already supported)*

**MSH — Marvel Super Heroes (12)**
- [ ] Dark Fortress · Gathering Place · Gleaming Bastion · Hidden Lair · Training Compound *(lands)*
- [x] Avengers Disassembled *(rare sorcery)*
- [ ] Hawkeye, Master Marksman · The Mighty Thor, Jane Foster · The Unbeatable Squirrel Girl ·
      Shang-Chi, Master of Kung Fu · M.O.D.O.K. *(legendary creatures; M.O.D.O.K. is an artifact creature)*
- [ ] Jennifer Walters // The Sensational She-Hulk *(transforming DFC, mythic)*

**DFT — Aetherdrift (3)**
- [x] Guidelight Optimizer · Nesting Bot · Marketback Walker *(artifact creatures)*
- [x] Howlsquad Heavy *(creature — Marauding Mako done)*
- [ ] Webstrike Elite *(creature)*
- [x] Monument to Endurance *(artifact — Lumbering Worldwagon, Perilous Snare, Repurposing Bay done)*
- [x] Momentum Breaker *(enchantment)*

**SPM — Marvel's Spider-Man (6)**
- [ ] Arachne, Psionic Weaver · Carnage, Crimson Chaos *(legendary creatures)*
- [ ] Supportive Parents *(creature)* · Multiversal Passage *(land)*
- [ ] Secret Identity · Spider-Sense *(instants)*

**MKM — Murders at Karlov Manor (5)**
- [ ] Novice Inspector *(common creature)* · Long Goodbye *(uncommon instant)* ·
      Deadly Cover-Up *(rare sorcery)*
- [ ] Case of the Crimson Pulse · Insidious Roots *(enchantments — Case is a "Case" enchantment,
      check whether the solve mechanic is already supported)*

**ZEN — Zendikar (1)**
- [ ] Bloodghast — the cube list's printing is SOC, but the canonical `card(...)` belongs in the
      **ZEN** set (2009-10-02); later printings are `Printing(...)` rows.

Re-derive this list before starting (sets ship and cards land continuously): paste the cube into the
deckbuilder's import modal and read the "Not implemented yet" panel, or use
`scripts/card-status --set <CODE> --cards`.
