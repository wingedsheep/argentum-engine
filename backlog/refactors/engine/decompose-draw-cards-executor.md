# Decompose DrawCardsExecutor

**Origin.** Tracked from Tier 2.1 of
[`2026-04-11_engine-improvement-plan.md`](./2026-04-11_engine-improvement-plan.md) — the single
largest executor in the tree (`rules-engine/.../handlers/effects/drawing/DrawCardsExecutor.kt`,
429 lines). This is the only 2.1 row that gets its own ticket; it is an engine-side refactor, not
an SDK composability concern.

## Problem

`DrawCardsExecutor` is conceptually four unrelated things welded together:

1. **Draw primitive** — take the top card of a library, move it to the owner's hand, track
   `CardsDrawnThisTurnComponent`, emit `CardsDrawnEvent`, detect empty-library loss via
   `PlayerLostComponent(LossReason.EMPTY_LIBRARY)` (respecting
   `GrantsCantLoseGameComponent`).
2. **Unified draw replacement shields** — Words of Worship / Wind / War / Waste / Wilding and
   similar, dispatched through `DrawReplacementShieldConsumer` at the start of each iteration.
   This is `checkStaticDrawReplacement` combined with the `shieldConsumer.consumeShield` path —
   both of which can pause via `DrawReplacementActivationContinuation` /
   `StaticDrawReplacementContinuation`.
3. **Prompt-on-draw activated abilities** — `checkPromptOnDraw` walks the active player's
   battlefield, finds `activatedAbilities` with `promptOnDraw = true`, solves mana, and produces a
   `SelectManaSourcesDecision` that pauses the draw mid-loop.
4. **Reveal-first-draw-of-turn** — `checkRevealFirstDraw` scans the battlefield for the
   `RevealFirstDrawEachTurn` static ability and emits `CardRevealedFromDrawEvent` for the first
   card drawn on a turn.

All four concerns share one `for (i in 0 until count)` loop, which is why the method is 140+ lines
with three separate "emit `CardsDrawnEvent` for the cards drawn *before* this pause" branches.
Every new draw-side-effect mechanic has to be threaded through the same loop, and every pause path
has to remember to flush the partial-draw event in the same way. A new contributor adding, say, a
"reveal each drawn card" mechanic has no obvious place to hook in.

There is also a duplicate of `checkPromptOnDraw` in `TurnManager` — the comment on line 258
explicitly says "same logic as `TurnManager.checkPromptOnDraw()` but for spell/ability draws."
Decomposing the executor lets both call sites share the same primitive.

## Target Shape

Split into three collaborators with single responsibilities. The SDK-level `DrawCardsEffect`
stays as a primitive (it *is* atomic from the card's point of view), but its executor becomes a
thin driver that sequences a pipeline of engine-internal services.

### `DrawCardPrimitiveExecutor`

Pure single-card draw. Takes `(state, playerId)`, returns `(newState, events, drawnCardOrNull)`.
Handles:

- Library-empty → `DrawFailedEvent` + conditional `PlayerLostComponent`.
- Zone move from `LIBRARY` → `HAND`.
- `CardsDrawnThisTurnComponent` increment.

Emits **one** `CardsDrawnEvent` per call. No loops, no replacement logic, no prompts. This is the
primitive that `DrawCardsExecutor`, `TurnManager` (draw step), and any future "investigate, draw a
card" mechanic can share.

### `DrawReplacementDispatcher`

Owns the "before drawing card N, check for things that intercept the draw" logic. Single entry
point:

```kotlin
fun checkBeforeDraw(
    state: GameState,
    playerId: EntityId,
    remainingDraws: Int,
    drawnSoFar: List<EntityId>,
    isDrawStep: Boolean
): DispatchResult
```

Returns either `None` (proceed with primitive draw), `Replaced(newState, events)` (shield
synchronously consumed a draw), or `Paused(ExecutionResult)` (decision emitted, resumption via
existing continuation types). Internally calls, in order:

1. `DrawReplacementShieldConsumer.consumeShield` (already exists).
2. `checkStaticDrawReplacement` (Parallel Thoughts style).
3. `checkPromptOnDraw` (activated abilities with `promptOnDraw = true`).

The executor doesn't care which of the three fired — it just sees `DispatchResult` and acts.

### `DrawCardsExecutor` (thin driver)

```kotlin
override fun execute(state, effect, context): ExecutionResult {
    val players = TargetResolutionUtils.resolvePlayerTargets(...)
    val count = amountEvaluator.evaluate(state, effect.count, context)

    var s = state
    val events = mutableListOf<GameEvent>()
    for (playerId in players) {
        repeat(count) { i ->
            when (val dispatch = replacementDispatcher.checkBeforeDraw(s, playerId, count - i, drawnSoFar, isDrawStep = false)) {
                is None      -> drawPrimitive.drawOne(s, playerId).also { s = it.state; events += it.events }
                is Replaced  -> { s = dispatch.state; events += dispatch.events }
                is Paused    -> return ExecutionResult.paused(dispatch.state, dispatch.decision, events + dispatch.events)
            }
        }
    }
    return ExecutionResult.success(s, events)
}
```

`checkRevealFirstDraw` moves to `DrawCardPrimitiveExecutor` since it's tied to "did this draw
increment `CardsDrawnThisTurnComponent` from 0 to 1" — i.e., it's a property of the primitive
draw, not the replacement dispatch.

## Why Not Go Further

Do **not** try to decompose `DrawCardsEffect` itself into `Gather(top 1) → Move(hand)`. Draw is
not just a zone change — it's the hook that replacement effects target (Rule 121.6), and draw
events are distinct from zone-change events in the engine. An SDK-level decomposition would mean
Words of Worship and friends have to reach into the pipeline and intercept a `MoveCollection`
step, which is a much worse coupling than the current replacement shield design.

## Files

- `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/drawing/DrawCardsExecutor.kt` — split
- `rules-engine/src/main/kotlin/com/wingedsheep/engine/handlers/effects/drawing/DrawReplacementShieldConsumer.kt` — already exists, becomes one of three branches in the dispatcher
- `rules-engine/src/main/kotlin/com/wingedsheep/engine/core/TurnManager.kt` — has duplicate `checkPromptOnDraw`; once the dispatcher exists, `TurnManager` should delegate to it

## Prerequisites

None — this is pure engine refactor. No SDK or card changes. Existing continuation types
(`DrawReplacementActivationContinuation`, `StaticDrawReplacementContinuation`) already carry the
state needed to resume draws mid-loop, so the wire protocol stays unchanged.

## Risk

Low. Every test that currently exercises a draw path should continue to pass byte-for-byte —
event ordering and `CardsDrawnEvent` aggregation must be preserved exactly (the three
"emit `CardsDrawnEvent` for cards drawn before this pause" branches today exist precisely to get
this right, and the new driver must still do the equivalent). Write a characterization test first
that fingerprints the current event sequence for the interesting cases before touching the code:

- Plain `DrawCardsEffect(3)` with no replacement effects.
- Same, with Words of Worship shield active (synchronous replacement, no pause).
- Same, with Words of Worship shield active and a pending mana decision (pause mid-loop).
- Same, with Parallel Thoughts prompting yes/no (pause mid-loop, first draw succeeded).
- Empty library on the 2nd of 3 draws, no `GrantsCantLoseGameComponent`.
- Empty library with `GrantsCantLoseGameComponent` (Platinum Angel) — no `PlayerLostComponent`.
- First draw of the turn under `RevealFirstDrawEachTurn` — expect `CardRevealedFromDrawEvent`
  before `CardsDrawnEvent`.
