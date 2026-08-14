# Emperor Variant (CR 809)

Add the **Emperor** multiplayer variant: two teams of three, each with one **emperor** flanked by two
**generals**. Nothing is shared (CR 809.7) — the team's fate is tied entirely to its emperor, and each
player can only reach part of the table. Teams win/lose with their emperor (CR 809.5), creatures may
only attack the seat next door (CR 809.3c), every creature gains a "give this to a teammate" ability
(CR 804), and — the headline engine work — each player has a **limited range of influence** (CR 801):
emperors 2 seats, generals 1.

Prerequisites already landed: [`multiplayer.md`](multiplayer.md) (Free-for-All, N-player engine/server/
client), Two-Headed Giant and Team vs. Team (`Format.TwoHeadedGiant` / `Format.TeamVsTeam`, team
membership, team loss propagation), and `AttackMode.LEFT` / `.RIGHT` (CR 803).

> **Note:** [`multiplayer.md`](multiplayer.md) § *Later — explicitly deferred* still says "Range of
> influence, attack-left/right, Grand Melee, team variants: **permanently** out of scope". Attack
> left/right and both team variants have since shipped; that line is stale and should be amended to
> point here when this lands.

---

## Why it's interesting

Emperor is the only variant in the CR that makes **board reach a resource**. A general can only touch
the enemy general across from them and their own emperor; the emperor sees both of its generals and
the two enemy generals but can't be attacked until a general falls. That produces a genuinely
different game (protect the fragile king, deploy creatures forward, trade tempo for reach) out of
*rules config* rather than new cards — which is exactly the axis this codebase is built along
(`Format` capability flags + per-player components, not code paths).

It is also the only remaining CR multiplayer variant of substance we don't support. Grand Melee
(CR 807, multiple simultaneous turns) is a much worse fit and stays out of scope.

---

## Rules baseline (verified against the CR, 2026-07-25)

### Emperor itself — CR 809

- **809.1 / 809.2** — two or more teams of three. Each team sits together; the **emperor sits in the
  middle**, the other two are **generals** whose job is to protect the emperor.
- **809.3a** — range of influence is **2 for emperors, 1 for generals** (CR 801).
- **809.3b** — Emperor games **always** use the deploy creatures option (CR 804).
- **809.3c** — a player can attack only an opponent **seated immediately next to them**, a
  planeswalker that opponent controls, or a battle they protect. The CR's own example: *at the start
  of an Emperor game neither emperor can attack anyone*, even though the enemy generals are inside
  their range of influence. Attack reach is **strictly narrower** than range of influence — they are
  two independent restrictions, not one.
- **809.4** — randomly determine which **emperor** goes first; turn order proceeds to the left.
- **809.5a/b/c** — a team **wins if its emperor wins**, **loses if its emperor loses**, and draws if
  the game is a draw for its emperor. A *general* being eliminated does **not** eliminate the team.
- **809.6 / 809.6a** — any number of equally sized teams; for teams larger than three, each general's
  range is the minimum that puts one enemy general in range and each emperor's range is the minimum
  that puts two enemy generals in range, with no emperor starting inside another emperor's range.
  The CR works the 4v4 case: seats `A-g1, A-emp, A-g2, A-g3, B-g1, B-emp, B-g2, B-g3`, emperors get
  3, `g2` gets 2, `g1`/`g3` get 1.
- **809.7** — resources are **not** shared (no shared life, no shared turns, no shared mana);
  teammates may review each other's hands and talk, but can't manipulate each other's cards.

### Deploy creatures — CR 804

- **804.2** — each creature has "**{T}: Target teammate gains control of this creature. Activate only
  as a sorcery.**" Every creature, all game, on every team.

### Limited range of influence — CR 801 (the big one)

- **801.2 / 801.2a–d** — range of influence is a distance in **player seats**. Players within that
  many seats are in range; objects controlled by an in-range player are in range; a battle is in
  range if its protector is. Different players may have different ranges. It covers "spells,
  abilities, effects, damage dealing, attacking, making choices, and winning the game."
- **801.2b** — a player is always within their own range.
- **801.2c** — the players within each player's range are **determined as each turn begins**. When a
  player leaves, their neighbours only close the gap at the start of the *next* turn. This is a
  snapshot, not a live computation.
- **801.3** — creatures can attack only in-range opponents / their planeswalkers / battles they
  protect; with no opponents in range, that player's creatures **can't attack at all**.
- **801.4** — out-of-range objects and players **can't be targeted**.
- **801.5a** — a player asked to choose an object or player must choose one in **their own** range
  (and, per the Cuombajj Witches example, one that also satisfies the asking effect's controller's
  range — so ranges intersect).
- **801.5b** — choosing between *modes/options* is unrestricted even if the options mention
  out-of-range objects.
- **801.5c** — if no player who can make a required choice is in the controller's range, the
  **closest appropriate player to the controller's left** makes it (worked example: Fact or Fiction
  cast by an emperor with range 1).
- **801.6** — a player **can't activate activated abilities of out-of-range objects**.
- **801.7 / 801.7a** — a triggered ability **doesn't trigger unless the trigger event happens
  entirely within its source's controller's range**. The Aura example: "whenever enchanted creature
  becomes blocked" triggers, but "…becomes blocked **by a creature**" doesn't, because the blocker is
  out of range. 801.7a: for events that move an object in or out of range, use the before/after game
  state per CR 603.6 / 603.10 (the Extractor Demon example).
- **801.8 / 801.9** — an Aura can't enchant out of range; Equipment/Fortification can't attach out of
  range. Both are **state-based actions** (Aura → owner's graveyard, Equipment → unattaches, stays).
- **801.10** — spells and abilities **can't affect** out-of-range objects/players; those parts of the
  effect do nothing and the rest works normally (Pyroclasm only hits creatures in range).
- **801.11** — a spell or ability **gets information only from within its controller's range**. Coat
  of Arms counts only in-range creatures — and, per the second example, it boosts a *teammate's*
  creatures using **the Coat of Arms controller's** range, not the teammate's.
- **801.12** — the "world" rule (CR 704.5k) applies only against in-range world permanents.
- **801.13 / 801.13a / 801.13b** — replacement and prevention effects can produce instructions that
  can't be carried out; those are ignored. Damage redirected to an out-of-range player is simply not
  dealt (the Lava Axe / Captain's Maneuver example: 2 to Rob, none to Carissa). Prevention shields
  scope by source and by recipient independently; a "prevent all damage dealt by creatures" static
  doesn't stop an out-of-range attacker, and Fog doesn't stop combat damage between two out-of-range
  creatures.
- **801.14** — "you win the game" makes only the **in-range** opponents lose instead.
- **801.15 / 801.16** — an effect-caused draw, or a mandatory loop, is a draw for the controller (or
  loop participants) and everyone in their range; **those players leave and the rest play on**.
- **801.17** — restart effects (CR 727) are **exempt**; all players join the new game.
- **801.18** — Planechase plane/phenomenon cards are exempt (not applicable — no Planechase support).

### Plus the general multiplayer rules already relied on

- **CR 800.4a–c** — the game continues when a player leaves; already implemented
  (`PlayerLeavesGameCheck` / `PlayerLeavesGameProcessor`).
- **CR 800.7** — the starting player does **not** skip their first draw step in multiplayer.
- **CR 101.4** — APNAP for simultaneous choices; already the engine's model.

---

## Survey — what already exists

The team and multiplayer scaffolding is in good shape. Range of influence is the one genuinely new
capability; everything else is a small parameter on machinery that's already there.

### Already supported ✓

| Need | Where it lives today |
|---|---|
| N-player state, turn order as a seat ring, cyclic neighbours | `GameState.turnOrder`, `getNextPlayer` / `getPreviousPlayer` (`state/GameState.kt`) |
| Team membership + team helpers | `TeamComponent` (`components/identity/PlayerIdentityComponents.kt`); `GameState.teams` / `teamOf` / `teammatesOf` / `teamActivePlayers` / `activeTeams` (`GameState.kt:509-605`) |
| Opponents exclude teammates | `GameState.getOpponents` (`GameState.kt:501`) |
| Format as capability-flag data, not a code path | `Format` + `sharesTeamLife` / `sharesTeamTurns` / `playersWinLoseAsTeam` (`mtg-sdk/.../core/Format.kt`) |
| Teams stamped at init; teams seated contiguously; starting team selection | `GameConfig.teams` / `startingPlayerIndex` (`core/GameInitializer.kt:82, 248-275`) |
| Seat-restricted attacking | `AttackMode` (`mtg-sdk/.../core/AttackMode.kt`), `CombatDefenders.legalDefendingPlayers`, `AttackModeDefenderRule` |
| Per-creature defenders, per-defender blocking in APNAP order | `CombatDefenders.defendingPlayersInApnapOrder`, `CombatEnumerator` |
| Team-scoped loss propagation as an SBA | `TeamLossPropagationCheck` (gated on `format.playersWinLoseAsTeam`) |
| Last-team-standing game end | `GameEndCheck` (already reasons over `activeTeams`) |
| Player leaving mid-game (CR 800.4) | `PlayerLeavesGameCheck` / `PlayerLeavesGameProcessor` |
| Granting an activated ability to a filtered group | `GrantActivatedAbility` static + `GrantActivatedAbilityToGroupExecutor` |
| 6-seat lobby, team assignment, single-pod lifecycle | `LobbyGameMode.TEAM_VS_TEAM` (`maxPlayers.coerceIn(4, 8)`), `FreeForAllHandler`, `TournamentLobby` |
| 6 seat colors, 2 team colors, team-split board, two-row table, overview mode, eliminated spectating | `web-client/src/styles/seatColors.ts`, `GameBoard.tsx`, `OpponentRail.tsx`, `boardViewSlice` |

### Missing ✗

1. **Range of influence — does not exist anywhere.** Zero hits for `rangeOfInfluence` in the repo.
   This is a new cross-cutting scoping concept touching targeting, activation, triggers, effect
   application, static-ability projection, information reads, attachments, replacement/prevention,
   and win/draw resolution.
2. **Player roles.** No notion of a designated team member whose fate is the team's.
   `TeamLossPropagationCheck` is all-or-nothing (any member's loss kills the team) — Emperor needs
   leader-only propagation.
3. **`AttackMode.ADJACENT`** (CR 809.3c — *both* neighbours, teammates excluded).
4. **Deploy creatures.** No "target teammate" player target in the SDK and no format-driven global
   ability grant.
5. **Turn order vs. seat ring.** `GameInitializer` builds turn order as `orderedTeams.flatten()`, so
   `turnOrder[0]` is a team's *first* member. Emperor needs the **emperor** to take the first turn
   (809.4) while the seat ring stays intact — i.e. a rotation of the ring, which makes a team's
   members **non-contiguous in `turnOrder`** (team A becomes seats `{0, 1, 5}`). Any code assuming
   teammates occupy a contiguous slice must handle the wrap.
6. **Lobby mode + role assignment** — `LobbyGameMode.EMPEROR` (exactly 6, or 2×N), plus who is the
   emperor on each team (host assignment or random).
7. **Client**: range-of-influence affordance (the single biggest UX risk — see Phase 5), emperor/
   general role badges, 3-per-team split layout, deploy-creatures ability surfacing.
8. **AI**: deferred, consistent with `multiplayer.md`. Emperor seats are humans-only at ship.

---

## Design

### Format and per-player data

```kotlin
// mtg-sdk — Format.kt
@Serializable
data class Emperor(
    val startingLife: Int = 20,
    val startingHandSize: Int = 7,
) : Format {
    // CR 809.7 — nothing is shared: sharesTeamLife / sharesTeamTurns stay false.
    // CR 809.5 — the team's fate follows its emperor, not "any member" (2HG) and not
    // "all members" (Team vs. Team), so playersWinLoseAsTeam stays false too.
    override val teamFateFollowsLeader: Boolean get() = true   // new flag, CR 809.5
    override val usesRangeOfInfluence: Boolean get() = true    // new flag, CR 801
    override val deployCreatures: Boolean get() = true         // new flag, CR 804
}
```

Three new default-`false` capability flags on `Format`, in the established style: every existing
format is unaffected because every flag defaults off, and Emperor is *data*.

Per-player state as two small components, both absent in every other format:

```kotlin
/** CR 801.2 — how many seats out this player can reach. Absent = unlimited. */
data class RangeOfInfluenceComponent(val seats: Int) : Component

/** CR 809.5 — this player's team wins/loses with them (the emperor). */
data object TeamLeaderComponent : Component
```

`GameConfig` grows `rangeOfInfluence: Map<Int, Int>? = null` and `teamLeaders: List<Int>? = null`
(indices into `players`, matching how `teams` already works), and validates the Emperor shape:
equally sized teams, one leader per team, leader in the middle seat, ranges assignable per 809.6a.
A helper computes the canonical 809.3a / 809.6a range for a given team size so the server and the
scenario builder don't hand-roll it.

### Range of influence as a per-turn snapshot

CR 801.2c is a gift: range is fixed at the start of each turn. So it becomes a cached map on
`GameState` rather than a seat-walk on every query:

```kotlin
/** CR 801.2c — players in each player's range, recomputed as each turn begins. Empty map when
 *  the format doesn't use limited range of influence (every query then answers "in range"). */
val playersInRange: Map<EntityId, Set<EntityId>> = emptyMap()
```

Recomputed in `TurnManager` at the turn-begin boundary from the *surviving* seat ring, so a departed
player's neighbours close the gap only at the next turn (matching the CR's Alex/Rob/Carissa example).
Single read surface, mirroring how `teamOf` / `sharedTurnTeam` are the only way to read `TeamComponent`:

```kotlin
fun inRangeOf(viewer: EntityId, other: EntityId): Boolean       // players
fun objectInRangeOf(viewer: EntityId, entityId: EntityId): Boolean  // via controller / battle protector
fun rangeFilter(viewer: EntityId): (EntityId) -> Boolean         // for enumerations
```

Every one returns `true` unconditionally when `playersInRange` is empty — that's what keeps the ~dozens
of new call sites free for Standard/Commander/FFA/2HG/TvT games and makes the change reviewable.

### Attack adjacency

Add `AttackMode.ADJACENT` (CR 809.3c) and one branch in `CombatDefenders.legalDefendingPlayers`:
`setOf(getNextPlayer(p), getPreviousPlayer(p)) - teamOf(p)`. Both the enumerator hint and
`AttackModeDefenderRule` already route through that function, so this is genuinely one branch. It
must yield an **empty set** for emperors at game start (CR 801.3's "then creatures can't attack"),
which `CombatEnumerator` needs to handle without producing a degenerate "declare attackers" action.

### Deploy creatures

Compose, don't invent: a format-gated global `GrantActivatedAbility` over "each creature on the
battlefield", granting `{T}: <target teammate> gains control of this permanent` with the existing
sorcery-speed activation restriction. The one SDK gap is a **teammate player target** —
`TargetFilter` currently has `AnOpponent` / `DefendingPlayer` and friends but no teammate arm. Add it
as a player-target arm (it's also the natural home for future team-variant cards), not a bespoke
effect. `GainControl` already exists.

Interaction to test: the ability targets a teammate, so CR 801.4 applies — a general can deploy to
its emperor (adjacent, in range) but **not** to the general on the far side of it (2 seats away,
range 1). That is the intended tension of the variant, not a bug.

### Emperor win/loss

Parameterize `TeamLossPropagationCheck` rather than adding a parallel check: when
`format.teamFateFollowsLeader`, propagate a loss to the whole team **only** when the lost player
carries `TeamLeaderComponent`, and never propagate from a general. `GameEndCheck` needs no change —
it already ends the game on one surviving team — but `activeTeams` should treat a team whose leader
is out as inactive so the game ends the moment an emperor dies even with generals still standing.
CR 801.14 / 801.15 layer on top: "you win the game" and effect-draws scope to the controller's range,
which is a change inside the win/draw resolution path, not the SBA.

---

## Phases

Phase 1–2 are self-contained and shippable behind the lobby mode. Phase 3 is the large one.

### Phase 1 — Format, roles, seating, win/loss

- `Format.Emperor` + the three capability flags; `GameConfig.rangeOfInfluence` / `teamLeaders` +
  validation and the 809.6a range calculator.
- `RangeOfInfluenceComponent`, `TeamLeaderComponent`, stamped in `GameInitializer`.
- Turn order: rotate the seat ring so the starting team's **leader** is the active player (809.4)
  while preserving cyclic adjacency; audit any assumption that teammates are contiguous in
  `turnOrder` (`GameInitializer`, `CombatDefenders`, the server roster/DTO builders, the client
  team-split layout).
- Leader-only loss propagation; `activeTeams` treats leaderless teams as out.
- Confirm CR 800.7 (no first-draw skip) holds for 6 players.
- Tests: `EmperorSetupTest` (seating, roles, ranges, first player is an emperor),
  `EmperorTeamLossTest` (general dies → team plays on; emperor dies → team out; last team wins),
  mirroring the `TwoHeadedGiant*Test` files.

### Phase 2 — Attack adjacency + deploy creatures

- `AttackMode.ADJACENT`; `CombatDefenders` branch; no-legal-defender handling in `CombatEnumerator`.
- `TargetFilter` teammate arm; format-gated global grant of the CR 804.2 ability.
- Tests: `EmperorAttackAdjacencyTest` (emperor can't attack turn 1; general attacks only the enemy
  general opposite; planeswalker/battle of an adjacent opponent is attackable),
  `EmperorDeployCreaturesTest` (deploy to emperor works, deploy to far general is an illegal target,
  sorcery-speed only, and a summoning-sick creature **can't** be deployed — CR 302.6 applies since
  the granted ability has `{T}` in its cost).
- Update [`docs/card-sdk-language-reference.md`](../docs/card-sdk-language-reference.md) for the new
  target arm (mandatory, same change).

### Phase 3 — Limited range of influence (CR 801)

The largest work item in this project by a wide margin; budget it as its own multi-session effort.
Each bullet is one enforcement seam, with the rule it implements:

- **Snapshot** — `GameState.playersInRange`, recomputed at turn begin in `TurnManager` (801.2c);
  `inRangeOf` / `objectInRangeOf` / `rangeFilter` helpers (801.2, 801.2b, 801.2d).
- **Targeting** (801.4) — target enumeration and target legality: `TargetResolutionUtils`, the target
  validators, `LegalActionEnumerator` / `CastSpellEnumerator`.
- **Activation** (801.6) — `ActivatedAbilityEnumerator`, `ActivateAbilityHandler`, and the other
  ability enumerators (`GraveyardAbilityEnumerator`, `CommandZoneAbilityEnumerator`, crew/saddle, …).
- **Attacking** (801.3) — intersect `CombatDefenders.legalDefendingPlayers` with range; note this is
  *in addition* to `AttackMode.ADJACENT`, and adjacency is the binding constraint in practice.
- **Delegated choices** (801.5a/b/c) — choices of objects/players intersect the chooser's and the
  effect controller's ranges; mode choices are unrestricted; fall back to the nearest appropriate
  player to the controller's left when nobody in range can choose.
- **Triggers** (801.7, 801.7a) — the deep one. `TriggerMatcher` / `TriggerProcessor` /
  `TriggerIndex` / the three detection entry points (`detectTriggers`,
  `detectPhaseStepTriggers`, `detectLeavesBattlefieldTriggers`) must gate on *every participant* in
  the trigger event being in the source controller's range, using the before/after state per
  CR 603.6 / 603.10 for zone-change events. Expect this to need a notion of "the objects this trigger
  event touched", which the event payloads may not fully carry today — survey `GameEvent` coverage
  before estimating.
- **Effect application** (801.10) — the fan-out sites: "each player", "each creature", `EachOpponent`,
  `AllPlayers`, `ForEachEffect` iteration spaces. Out-of-range parts do nothing; the rest resolves.
- **Information reads** (801.11) — `StateProjector`, `PredicateEvaluator` (including
  `matchesWithProjection`), `DynamicAmountEvaluator`, and count/collection sites must scope to the
  *reading spell/ability's controller's* range, not the affected player's. The Coat of Arms example is
  the canonical test. **Perf risk**: `StateProjector` runs constantly; the range check must be an
  O(1) set lookup off the snapshot, and static-ability projection may need per-controller caching.
- **Attachment SBAs** (801.8, 801.9) — extend the existing aura-legality SBA and add the
  Equipment/Fortification unattach case.
- **World rule** (801.12) — scope the CR 704.5k check to in-range world permanents.
- **Replacement / prevention** (801.13, 801.13a, 801.13b) — impossible instructions ignored; damage
  redirection to out-of-range recipients dealt to nobody; prevention shields scoped independently by
  source and by recipient.
- **Winning / drawing / loops** (801.14, 801.15, 801.16) — "you win" → only in-range opponents lose;
  effect-draws and mandatory loops remove the controller plus their range and the game continues.
- **Restart exemption** (801.17).
- Tests: an `EmperorRangeOfInfluence*Test` family, one per sub-rule, seeded directly from the CR's own
  worked examples (Cuombajj Witches, Runeclaw Bear + two Auras, Extractor Demon, Pyroclasm, Coat of
  Arms ×2, Lava Axe + Captain's Maneuver, the three prevention examples, Fact or Fiction). Those
  examples are the acceptance criteria — implement against them literally.

### Phase 4 — Server: lobby mode + session

- `LobbyGameMode.EMPEROR` alongside `TEAM_VS_TEAM`: exactly 6 by default (2 teams of 3), optionally
  2 teams of N. Reuses the `FreeForAllHandler` single-pod lifecycle, team-assignment plumbing, and
  pool building (sealed / draft / custom decks) — same as 2HG and TvT did.
- Role assignment: host picks each team's emperor, or random; seat ordering enforces middle-seat
  emperors and the 809.6a range assignment before handing `GameConfig` to the engine.
- DTOs: add role and range to the game-start roster (the same two places `AvailableSet` taught us to
  update — see the extension-sets note). Reject AI seats (AI deferred).
- Scenario builder + hotseat: expose Emperor as a scenario mode so the dev loop can drive all 6 seats
  from one client, which is how Phases 1–3 get exercised end-to-end without 6 humans.
- Tests: `EmperorLobbyTest` (6-player premade pod end-to-end over WebSockets), `EmperorSessionTest`,
  `EmperorScenarioTest`.

### Phase 5 — Client

- **Range-of-influence affordance** — the primary UX problem, and the reason to prototype before
  building: a player must be able to tell at a glance *what they can touch*. Starting proposal:
  out-of-range seats render with a desaturated / dimmed board and a "out of your reach" rail chip
  state (reusing the existing dimmed 🚫 attack-restriction chip language from the multiplayer UI
  pass), plus out-of-range permanents non-interactive with an explanatory tooltip on hover. Trace the
  player flow before committing — per the UX-review habit, prefer making the *board* express the
  restriction over adding another banner.
- **Role badges** — emperor vs. general on nameplates and rail chips; the emperor's board is the one
  everyone watches, so it deserves visual weight (crown on the plate, ring on the board).
- **Layout** — 3-per-team split; the two-row table and team-split layouts already exist for 2HG/TvT,
  and 6 seat colors + 2 team colors are already defined, so this is extension rather than new
  geometry. `TEAM_COLORS` stays at two entries (Emperor is 2 teams by default).
- **Deploy creatures** — every creature gaining an activated ability makes the ability-button UI much
  busier; check it doesn't drown out real abilities (a distinct "deploy" affordance on the card, or
  drag-to-teammate, may read better than another ability button).
- **Attack clarity** — the existing attack-restriction banner + dimmed rail chips extend to
  `ADJACENT`; the emperor's "you cannot attack anyone" state needs an explicit, non-alarming message.
- Playwright: one 6-seat Emperor scenario game (blocked on the multi-connection N-seat fixture that
  [`multiplayer-ui-followups.md`](multiplayer-ui-followups.md) § 1 already needs).

### Deferred

- **AI seats** — Emperor is worse for the AI than FFA: range-limited board evaluation plus a "protect
  the emperor" objective that isn't expressible as a life differential. Humans-only at ship; hotseat
  and the scenario builder cover dev testing.
- **Teams larger than three** (809.6) — the range calculator should implement 809.6a generally, but
  the lobby ships 2×3 only. 4v4 needs 8 seats and 8 seat colors.
- **Grand Melee (CR 807)** — permanently out of scope.
- **Emperor + Commander** — should compose for free (format is a deck/config axis, mode is a seating
  axis), but not a ship gate.

---

## Definition of done

- [ ] 6 humans join a lobby, get seated as two teams of three with middle-seat emperors, and play one
      Emperor game to completion
- [ ] An emperor's death ends the game for that team immediately, with generals still on the
      battlefield; a general's death does not (809.5a/b)
- [ ] Neither emperor can declare an attacker on turn one; a general can attack only the enemy
      general opposite them (809.3c, with the CR's own example as the test)
- [ ] Deploy creatures works: a general hands a creature to its emperor, and cannot hand it to the
      far general (804.2 + 801.4)
- [ ] Every worked example in CR 801 has a passing test (see Phase 3)
- [ ] Out-of-range boards and permanents are visibly unreachable in the client, and attempting to
      interact explains why
- [ ] Every non-Emperor experience is byte-identical: the three new `Format` flags default false,
      `playersInRange` is empty, and the full `:rules-engine` + `:game-server` + client suites are
      green (the honest gate — this project adds range checks to dozens of hot paths)
- [ ] `docs/card-sdk-language-reference.md` updated for the teammate target arm; the stale
      "permanently out of scope" line in `multiplayer.md` amended

---

## Risks & unknowns

- **Phase 3 is the project.** Range of influence is a scoping concept that cuts across targeting,
  triggers, projection, information reads, replacement effects and win conditions — it is closer in
  size to the layer system than to a mechanic. The `playersInRange`-empty fast path is what keeps it
  from being a rewrite, but it still means touching many hot files. Consider landing Phases 1, 2 and 4
  first with range unrestricted (an "Emperor without 801" dev config), so the seating / roles /
  win-loss / attack / deploy layer is proven and playable before 801 starts. That intermediate state
  is **not** rules-legal Emperor — emperors could reach the whole table — so it must stay behind a dev
  flag, never a shipped lobby mode.
- **Trigger scoping (801.7) may not be expressible with today's event payloads.** "The trigger event
  happened *entirely* within range" requires knowing every object a given event touched. Survey
  `GameEvent` field coverage before estimating; the "blocked" vs. "blocked by a creature" distinction
  in the CR's example is exactly the sort of thing our events may collapse.
- **`StateProjector` performance.** 801.11 puts a per-controller range filter inside the projection
  path, which already dominates engine cost (see [`engine-performance.md`](engine-performance.md)).
  Measure before and after; per-controller projection caching may become mandatory rather than
  optional.
- **Non-contiguous teams in `turnOrder`.** Rotating the seat ring so an emperor goes first splits a
  team across the wrap. Anything that assumed `teams` come out as contiguous slices (init, the server
  roster, the client team-split layout) needs an audit — this is a quiet source of off-by-one bugs.
- **UX legibility.** A 6-seat table where each player sees a different subset of it is the hardest
  multiplayer UI we'd have shipped. If the range affordance isn't obvious, the variant reads as
  "the game is broken" rather than "that's the rule". Prototype the affordance early; it may
  justify reordering Phase 5 ahead of parts of Phase 3.
- **Player supply.** Emperor needs six humans in a lobby. Realistically the variant gets played via
  hotseat and scenario testing far more than live, which is worth weighing against Phase 3's cost —
  a 3v3 Team vs. Team game (already supported) delivers "6-player teams" today without any of 801.
- **Replay / newspaper tooling** already flagged as 1v1-shaped in `multiplayer.md`; a 6-seat
  range-limited game is further from what `tournament-newspaper` reconstructs. Not a blocker.

---

## References

- CR 801 (Limited Range of Influence), 802 (Attack Multiple Players), 803 (Attack Left/Right),
  804 (Deploy Creatures), 808 (Team vs. Team), 809 (Emperor) —
  <https://magic.wizards.com/en/rules>
- [`multiplayer.md`](multiplayer.md) — the N-player foundation (Free-for-All, CR 806/802)
- [`multiplayer-ui-followups.md`](multiplayer-ui-followups.md) — overview mode, combat split,
  eliminated spectating, and the multi-connection e2e fixture this project also needs
- [`docs/web-client-architecture.md`](../docs/web-client-architecture.md) § *Multiplayer (3-4 player)
  board*
- [MTG Wiki — Emperor](https://mtg.wiki/page/Emperor)
