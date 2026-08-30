# Playing the gym server against itself (manual set testing)

A way for **Claude Code (or any human/agent) to drive a full game of Magic over HTTP**, making
every decision for both players, to shake out cards in a new set that don't behave as printed.

This is *exploratory* testing, not a replacement for scenario tests. A scenario test asserts one
known interaction; a self-play session walks an entire game and surfaces the things you didn't think
to write a test for — a card that produces no legal action, an oracle effect that never fires, a
trigger that crashes the engine, a board state that can't legally exist.

The engine never picks moves. The `gym-server` exposes a stateless step loop: it answers *"what are
the legal actions?"* and *"what's the state after this action?"*, and the caller chooses. That makes
the engine perfectly inspectable from the outside — which is exactly what we want for finding bugs.

> Endpoint and payload reference is in this doc; the source of truth is
> [`gym-server/.../EnvController.kt`](../gym-server/src/main/kotlin/com/wingedsheep/gym/server/controller/EnvController.kt)
> and [`gym/.../TrainingObservation.kt`](../gym/src/main/kotlin/com/wingedsheep/gym/contract/TrainingObservation.kt).
> Live, try-it-out docs are at `http://localhost:8081/swagger-ui.html` once the server is up.

---

## 1. Start the server

```bash
just gym-server        # ./gradlew :gym-server:bootRun — listens on :8081
```

Wait for it to come up, then sanity-check:

```bash
curl -s localhost:8081/health        # -> {"status":"ok"}
```

The server is in-memory and unauthenticated — bind to localhost, and remember a restart wipes every
env.

---

## 2. Create a game

`POST /envs` with an `EnvConfig`. Build a deck that **concentrates the cards under test** — a small
toolbox of the new set's cards plus enough basics to actually cast them. The point is to maximise the
chance each card you care about gets drawn and played, not to build a good deck.

```bash
curl -s -X POST localhost:8081/envs -H 'Content-Type: application/json' -d '{
  "players": [
    { "name": "A", "deck": { "type": "Explicit", "cards": {
        "Mountain": 14, "Raging Goblin": 4, "Some New Set Card": 4, "Another New Card": 4
    } } },
    { "name": "B", "deck": { "type": "Explicit", "cards": {
        "Mountain": 14, "Raging Goblin": 4, "Some New Set Card": 4, "Another New Card": 4
    } } }
  ],
  "skipMulligans": true,
  "startingPlayerIndex": 0,
  "revealAll": true
}'
```

Key config fields:

- **`deck`** — `{"type":"Explicit","cards":{"Name":count}}` (recommended for testing), or
  `{"type":"RandomSealed","setCode":"BLB","boosterCount":8}` (needs the set's basic-land variants
  registered).
- **`revealAll: true`** — set this for self-play. Normally observations hide the opponent's hand and
  libraries except for identities the current perspective is entitled to know; since one agent is
  playing *both* seats, you want to see everything. (Never use it for real RL self-play — it leaks
  information.)
- **`skipMulligans: true`** — skip the mulligan back-and-forth.
- `startingPlayerIndex` — pin it for reproducibility (null = random).

The response is `{ "envId": "...", "observation": { ... } }`. Keep the `envId`.

---

## 3. The decision loop

```
loop:
  read observation
  if observation.terminated  -> done (winnerId)
  if observation.pendingDecision != null and pendingDecision.requiresStructuredResponse:
        POST /envs/{id}/decision  with a typed DecisionResponse   (section 5)
  else:
        pick an actionId from observation.legalActions
        POST /envs/{id}/step  { "actionId": N }
  -> response is the next observation; repeat
```

```bash
curl -s -X POST localhost:8081/envs/$ENV/step \
  -H 'Content-Type: application/json' -d '{"actionId": 3}'
```

**Action IDs are per-step.** They are regenerated on every step/decision. Always pick from the
*latest* observation; a stale id returns `400`.

### Actions that need more than an ID

Some enumerated actions are *templates*: the engine offers one `DeclareAttackers` action carrying an
**empty** attacker map, and advertises the candidates on the same entry. Stepping it by ID alone is
a legal move that declares no attackers — so combat happens only if you say who attacks. The same
holds for blocks, for a spell's targets, and for X.

Send those choices as `params`:

```bash
# Attack: every eligible creature at the first legal defender.
curl -s -X POST localhost:8081/envs/$ENV/step -H 'Content-Type: application/json' -d '{
  "actionId": 4,
  "params": { "attackers": { "<attackerId>": "<defenderId>" } }
}'

# Block: one blocker onto one attacker.
curl -s -X POST localhost:8081/envs/$ENV/step -H 'Content-Type: application/json' -d '{
  "actionId": 2,
  "params": { "blockers": { "<blockerId>": ["<attackerId>"] } }
}'

# Cast with targets and X.
curl -s -X POST localhost:8081/envs/$ENV/step -H 'Content-Type: application/json' -d '{
  "actionId": 7,
  "params": { "targets": ["<entityId>"], "xValue": 3 }
}'
```

| `params` field | Shape | Comes from |
|---|---|---|
| `attackers` | attacker id → defender id | `validAttackers`, `validAttackTargets` on the action |
| `blockers` | blocker id → `[attacker id, …]` | `validBlockers`, `blockerMaxBlockCounts`, `mandatoryBlockerAssignments` on the action; attackers from the board |
| `targets` | `[entity id, …]`, in requirement order | `targetEntityIds`, `minTargets`, `maxTargets` |
| `xValue` | int | `hasXCost`, `maxAffordableX` |

The declaration constraints are not advisory — a declaration that disobeys one is illegal, and the
step is rejected. `mandatoryAttackers` lists creatures that must attack if able (CR 508.1d);
`mandatoryBlockerAssignments` lists blocks that must be made if able (CR 509.1c); and
`blockerMaxBlockCounts` caps how many attackers a blocker may block at once, where absent means the
default one (CR 509.1a). Params a given action can't use, and a declaration the engine rejects, both
return `400` with the reason — neither is silently dropped, on `/step` or on `/step-batch`. Anything
richer (bands, alternative-cost payments, convoke/delve selections) is not expressible over `step`;
complex decisions go to `POST /envs/{id}/decision` (section 5).

### Reading an observation

The fields that matter most for spotting bugs:

- `agentToAct` — whose decision this is. (With `revealAll` you make moves for both.)
- `legalActions[]` — each has `actionId`, `kind` (the engine's action type verbatim: `CastSpell`,
  `PlayLand`, `ActivateAbility`, `DeclareAttackers`, `DeclareBlockers`, `PassPriority`, `DECISION`,
  …), `description`, `affordable`, `manaCost`, target counts, and the combat candidates
  (`validAttackers`, `mandatoryAttackers`, `validAttackTargets`, `validBlockers`,
  `blockerMaxBlockCounts`, `mandatoryBlockerAssignments`).
- `zones[]` → `cards[]` → `EntityFeatures` — the projected (post-layers) truth about each visible
  object. A zone can remain `hidden: true` while `cards` contains its individually known subset:
  `oracleText`, `power`/`toughness`, `types`/`subtypes`/`keywords`/`colors`, `tapped`, `counters`,
  `attachedTo`. **`oracleText` is your oracle**: read what the card *says*, then watch whether the
  game state changes the way it says.
- `stack[]` — what's resolving, with `oracleText` and `targets`.
- `players[]` — life, hand/library/graveyard sizes, mana pool.
- `stateDigest` — a hash of the observable state; if it doesn't change across a full round of passes,
  you're in a loop (see section 6).

---

## 4. How to reason through moves (and what to look for)

Play *purposefully toward exercising the cards under test*, not to win:

1. **Develop mana**, then **cast the new-set cards** as soon as they're castable. Prefer the action
   whose `description`/`sourceEntityId` matches a card you want to test.
2. When a spell or ability resolves, **diff the before/after observation against its `oracleText`**:
   - Did the stated effect happen (counters added, cards drawn, creature destroyed, life changed)?
   - Did the *right* objects change? (Use `entityId`s — projection means types/P-T can differ from
     the printed card.)
   - Did a triggered ability that should have fired appear on the `stack`?
3. **Drive combat** to exercise attack/block/damage triggers and keywords (flying, trample, first
   strike, deathtouch) — declare attackers and blockers with `params` (section 3), not with a bare
   `actionId`, which declares none.
4. **Pass priority** (`kind: "PassPriority"`) when there's nothing to test, to advance the turn.

Red flags that mean a card is probably broken — note them, don't just play around them:

- A card in hand that should be castable but produces **no `PLAY_CARD` action** (or one with
  `affordable: false` when you can clearly pay).
- A resolved spell whose **oracle effect didn't change the state** (silent fizzle).
- A trigger condition that's met but **nothing lands on the stack**.
- An **HTTP 500** (engine exception) or **409** on a decision — capture the request that caused it.
- `EntityFeatures` that contradict the card: missing/extra keywords, wrong P/T after a pump, a
  type-changing effect not reflected, counters not applied.
- A board state that **can't legally exist** (e.g., an Aura on the battlefield attached to nothing).
- The game **stalls** — same `stateDigest` cycling forever (often a never-ending trigger loop).

When you find one, record: the `envId`, the card, the action/decision posted, the observation before
and after, and what oracle text predicted. That reproduction is what becomes a scenario test or bug
report.

---

## 5. Structured decisions

Simple decisions (yes/no, choose-a-number, choose-a-mode, single-select) are **folded into
`legalActions`** as entries with `kind: "DECISION"` — just `step` the chosen `actionId`.

Complex decisions set `pendingDecision.requiresStructuredResponse = true` and leave `legalActions`
empty. Post a typed `DecisionResponse` to `POST /envs/{id}/decision`. The JSON discriminator key is
`"type"` and its value is the response's `@SerialName`. Match the response to
`pendingDecision.kind`:

| `pendingDecision.kind` | `type` (SerialName) | Payload fields |
|---|---|---|
| `CHOOSE_TARGETS` | `TargetsResponse` | `selectedTargets: { "<reqIndex>": ["<entityId>", …] }` |
| `SELECT_CARDS` | `CardsSelectedResponse` | `selectedCards: ["<entityId>", …]` |
| `YES_NO` | `YesNoResponse` | `choice: true\|false` |
| `CHOOSE_MODE` | `ModesChosenResponse` | `selectedModes: [int, …]` |
| `CHOOSE_COLOR` | `ColorChosenResponse` | `color: "RED"` |
| `CHOOSE_NUMBER` | `NumberChosenResponse` | `number: int` |
| `DISTRIBUTE` | `DistributionResponse` | `distribution: { "<entityId>": int }` |
| `ORDER_OBJECTS` | `OrderedResponse` | `orderedObjects: ["<entityId>", …]` |
| `SPLIT_PILES` | `PilesSplitResponse` | `piles: [["<id>",…], ["<id>",…]]` |
| `CHOOSE_OPTION` | `OptionChosenResponse` | `optionIndex: int` |
| `CHOOSE_REPLACEMENT` | `ReplacementChosenResponse` | `fromIndex: int, toIndex: int` |
| `ASSIGN_DAMAGE` | `DamageAssignmentResponse` | `assignments: { "<entityId>": int }` |
| `SELECT_MANA_SOURCES` | `ManaSourcesSelectedResponse` | `selectedSources: ["<id>",…]`, `autoPay: true`, or `declined: true` |
| `BUDGET_MODAL` | `BudgetModalResponse` | `selectedModeIndices: [int, …]` |

Every response also needs `decisionId` (copy it from `pendingDecision.decisionId`). The
`pendingDecision.shape` field carries the constraints (`minSelections`, `maxSelections`,
`numericMin/Max`, `availableColors`, `totalToDistribute`, `budget`).

```bash
# Choose targets for a pending ChooseTargets decision (requirement 0 -> one creature)
curl -s -X POST localhost:8081/envs/$ENV/decision -H 'Content-Type: application/json' -d '{
  "type": "TargetsResponse",
  "decisionId": "'"$DID"'",
  "selectedTargets": { "0": ["'"$TARGET"'"] }
}'
```

Source of truth for these shapes:
[`PendingDecision.kt`](../rules-engine/src/main/kotlin/com/wingedsheep/engine/core/PendingDecision.kt)
(the `sealed interface DecisionResponse`).

---

## 6. Practical guardrails

- **Cap the loop.** A real game is hundreds of priority passes. Set a max-step budget (e.g. 1500) and
  abort if exceeded — a runaway count is itself a finding (probably a trigger loop).
- **Detect stalls** via `stateDigest`: if a full round-trip of passes by both players doesn't change
  it and the game isn't over, you're looping.
- **Reproducibility:** pin `startingPlayerIndex` and keep the exact decklist. The engine's own
  randomness (draws) isn't seedable here, so capture the full action transcript to replay a bug.
- **Snapshot/fork** before a risky line: `POST /envs/{id}/snapshot` → `{handle}`, then
  `POST /envs/{id}/restore {"handle": …}` to retry a different decision from the same point. Useful
  for testing both branches of a modal/choice without replaying the whole game.
- **Clean up:** `curl -X DELETE localhost:8081/envs -d '{"envIds":["'"$ENV"'"]}'`.

---

## 7. Turning a finding into a regression test

A self-play bug is only worth as much as its reproduction. Once you isolate one:

1. Note the minimal board state right before the broken action (from the observation).
2. Reproduce it as a Kotest **scenario test** (`ScenarioTestBase`) or a **manual scenario JSON**, not
   another self-play run — see [`add-card`](../.claude/skills) conventions and existing
   `rules-engine` scenario tests.
3. Fix the card/engine, and leave the scenario test behind so it can't regress.
