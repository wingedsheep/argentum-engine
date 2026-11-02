# Player input and live response ownership

The engine runs synchronously until it reaches a player decision, a terminal state, or an error.
It stores the data needed to resume; no coroutine or closure is retained in `GameState`.

## Engine suspension

A question producer calls `GameState.suspendForDecision` with a question factory and an
`AnswerContinuation` payload. The operation allocates one deterministic routing ID, creates a
`Suspension(question, answer)`, installs it on the continuation stack, and emits a
`DecisionRequestedEvent`. `GameState.pendingDecision` reads the question from the top suspension.

Surrounding execution uses `ExecutionResult.propagatePause` to carry that existing suspension.
It does not allocate another ID or emit another request. Automatic work beneath a question uses
`AutomaticContinuation` frames, which have no response address. The stack determines their order.
A mana ability inside a payment window sets aside the whole suspension, then restores it with the
same ID and refreshed source menu after the nested execution finishes.

`SubmitDecisionHandler` validates the acting player and choice payload against the pending
question. `ContinuationHandler` verifies the response ID, pops the suspension, and dispatches its
answer data. The paired question is available to resumers that need its original shape, including
combat assignment. Legal options and their validation remain engine responsibilities.

## Live transports

The browser receives masked state and server-computed options. AI updates also retain the live
origin from their particular snapshot, including through asynchronous thinking and approval.
Both adapters submit a canonical engine action with that origin through `LiveActionSubmission`.
`GameSession.executeLiveAction` checks freshness and executes under one session lock.

Engine question IDs reproduce when the same saved execution is replayed. Live undo independently
rotates the session's interaction epoch, invalidating deliveries from the abandoned branch even
when the restored counter produces the same question ID. Browser decisions encode the epoch in
the opaque question token; ordinary browser actions send it in their envelope. AI keeps raw engine
IDs for simulations and sends the epoch separately. Neither adapter supplies a newer epoch at
response time. See [data contracts](data-contracts.md) for transport compatibility and recovery.

## Saved states

Snapshots serialize question and answer together. The companion legacy reader pairs an old
pending question with its matching top answer and recovers temporarily hidden mana questions
from their saved reopen frames. It retains gameplay state and counters, rejects malformed
associations, and writes only the current format. Translation passes through the current-format
rejection check before decoding.

Without that reader the engine rejects the previous representation outright rather than guessing,
so a deployment that must resume already-saved paused games needs it. There is no runtime feature
flag. See [architecture principles](architecture-principles.md#24-reentrant-continuations).

### Optional pile membership

`SplitPilesDecision` normally assigns every card to exactly one pile. `allowUnassigned` permits
omitting cards, and `maxPileMemberships` declares each card's maximum number of distinct piles
(default one). Duplicate cards within one pile and unknown cards are always rejected. `useTargetingUI`
requests a battlefield selection banner rather than the hidden-card pile overlay. These fields are
server-authored constraints; they do not ask the client to determine blocking legality.

Randomized blocker declarations suspend the turn-based declaration in this question. The continuation
captures the current attacker list, advances RNG only after a valid answer, and preserves the resulting
assignment across any restriction-choice question. Completing it marks the defender's declaration and
hands input directly to the next undeclared defender before any priority window.

For a constrained split, `pileOptions` limits the cards eligible for each pile and
`requiredAssignments` fixes the total number of memberships. `suggestedPiles` supplies one
server-validated plan for automated responders. After randomized assignment, these fields let the
player choose a legal subset on the battlefield without rerolling or enumerating every combination.
The continuation also checks the resulting combat restrictions before committing any block.

### Repeated battlefield selections

A `SelectCardsDecision` with `selectionLimits` assigns a maximum number of units to each
offered permanent. A response repeats its entity ID once per unit; typed counter-removal
payments use this to distribute a fixed cost across permanents. The engine validates each
capacity and requires the full `maxSelections` total or an empty response to decline.
The client renders allocation controls on the battlefield, using only these supplied limits.
Absent `selectionLimits`, ordinary selection behavior is unchanged.

### Suggested selections

A `SelectCardsDecision` may carry `recommendedSelection` — a legal subset of `options` the engine
suggests for the deciding player. It is advice only: validation ignores it and the player may submit
any legal subset. The battlefield selection banner offers it as a one-click **Recommended** pick next
to **All** (shown when every option fits under `maxSelections` and no per-type, colour, name, power
or mana-value constraint couples the picks) and **Clear**; the player adjusts on the board before
confirming. Proliferate fills it (`ProliferateExecutor.recommendedRecipients`): you, your permanents
and a teammate's unless they carry a counter that hurts their holder (-1/-1, stun, poison, rad, age,
defense), and opponents and their permanents only when they carry such a counter and none that helps
them (+1/+1, loyalty, shield, keyword counters).
