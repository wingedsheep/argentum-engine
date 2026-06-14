# Stack collapse + batch decisions — taming high-volume stacks

_Drafted 2026-06-14. Status: C.2 (the shared `AbilityIdentity` key, PR #720) and **B (batch
may-question, PR #725)** implemented; A (visual collapse) and C (persistent yields) still
spec-only. Covers three independent but related features — **A. visual stack collapse**
(client-only), **B. batch decision answering** (engine + client), **C. persistent per-ability
yields** (engine + server + client). A is shippable on its own; B and C share an "ability identity
key" and a yes/no batch path, and C builds on B's machinery._

## 0. The problem

When many objects land on the stack at once — storm/copy effects, a board of identical ETB or
upkeep triggers, a token swarm with a shared "whenever a creature you control …" trigger — the
current UX degrades in two distinct ways:

1. **The stack is unreadable.** `StackZone.tsx` renders every stack entity as an overlapping card
   offset 25px from the previous one. Twenty copies of the same trigger is twenty cards fanned
   across the board with no summary; you can't tell at a glance "this is the same thing ×20."
2. **The controller is forced to click through repeated decisions.** Each *optional* ("you may")
   trigger raises its own yes/no decision while being put on the stack
   (`TriggerProcessor.processMayThenTargetTrigger`), and each stack item resolves under its own
   priority window. For N identical triggers that's N near-identical prompts plus N priority
   passes — the "decline endlessly or time out" failure mode Arena is criticised for.

We want two affordances the established clients already have:
- **Collapse** identical stack items into one summary entry with a count.
- **Accept all of a given type / accept all** — answer one repeated decision once, optionally
  persisting the answer so it stops asking.

## 1. Prior art (what other clients do)

- **Magic Online** is the reference implementation, and it cleanly separates the two problems:
  - **Auto-stacking identical triggers** (`7`): puts *all abilities that trigger at the same time,
    have identical text, and require no targets* onto the stack at once, instead of one prompt
    each. Note the strict guard — **same text + no targets**. Anything that targets or makes a
    choice still prompts individually. We adopt this guard verbatim; it is the safety boundary for
    feature B.
  - **Persistent yields** (right-click an ability): *"Yield to [effect] until end of turn"*,
    *"Always yield to [effect]"*, *"Always yes/no to [effect]"*. The answer is remembered and keyed
    by the ability's identity (so it applies to every copy / future instance), not by the specific
    object on the stack. This is feature C.
  - **Priority skips** (`2` pass-until-respondable, `6` yield-through-turn, `8` no-possible-play →
    yield-all). We already have the server-side equivalent in `AutoPassManager`; out of scope here
    except where B/C interact with it.
- **MTG Arena** is the weaker example and mostly tells us *what not to do*: no manual trigger
  ordering, and big/looping stacks force decline-spam or timeouts.
- **General product UX** (PatternFly bulk-selection, Material selection, eBay/Eleken bulk-action
  bar) gives the visual grammar for A and B: fold repeated rows into one summary row with a count,
  expandable on demand; surface batch verbs in a contextual bar once a group exists; and make the
  scope explicit — *this kind* vs *all*.

Sources:
[MTGO tips & tricks](https://www.mtgo.com/getting-started/getting-started-tips-tricks) ·
[Arena hotkeys](https://mtgazone.com/arena-hot-keys-and-interface-guide-simplify-your-game-with-these-easy-tricks/) ·
[WotC feedback: excessive triggers](https://feedback.wizards.com/forums/918667-mtg-arena-bugs-product-suggestions/suggestions/45327478-players-with-excessive-triggers-on-the-stack-will) ·
[Bulk-action UX (Eleken)](https://www.eleken.co/blog-posts/bulk-actions-ux) ·
[PatternFly bulk selection](https://www.patternfly.org/patterns/bulk-selection/) ·
[Material selection patterns](https://m1.material.io/patterns/selection.html)

## 2. Where this lands in the current code

Confirmed seams (so the spec is grounded, not guessed):

| Concern | File | Notes |
|---|---|---|
| Stack entity components | `rules-engine/.../state/components/stack/StackComponents.kt` | `SpellOnStackComponent` (`casterId`), `TriggeredAbilityOnStackComponent` (`sourceName`, `description`/`descriptionOverride`, `controllerId`, `copyIndex`/`copyTotal`), `ActivatedAbilityOnStackComponent` |
| Stack rendering | `web-client/src/components/game/board/StackZone.tsx` | `StackDisplay()` fans cards at a fixed 25px offset; reads `useStackCards()` selector over the STACK zone; renders `ClientCard` badges |
| Stack client shape | `web-client/src/types/gameState.ts` | `ClientCard` (`id`, `name`, `controllerId`, `copyIndex`, `chosenX`, …) |
| Trigger → stack, may-question | `rules-engine/.../event/TriggerProcessor.kt` | `processTriggers()` runs triggers **one at a time in APNAP order**; `processMayThenTargetTrigger()` raises the per-trigger yes/no; a mid-run pause stores the rest on `PendingTriggersContinuation` |
| Decision types | `rules-engine/.../core/PendingDecision.kt` | `YesNoDecision`, `ChooseTargetsDecision`, `SelectCardsDecision`, … — no batch variant today |
| Priority auto-pass | `game-server/.../priority/AutoPassManager.kt` | `shouldAutoPass(...)` with `stopsMode` + per-step `myTurnStops`/`opponentTurnStops`; **no per-ability yield memory** |

Two facts shape the design:
- **Trigger ordering is fully automatic (APNAP) today** — the engine never asks the player to order
  simultaneous triggers, so collapsing/batching never has to reconstruct an ordering decision.
- **The controller's own stack items already largely auto-resolve** via `AutoPassManager` (Rule 4:
  your own spell/ability on top → auto-pass). So "resolve all" friction for the *controller* is
  mostly the **may-question spam at put-on-stack time**, not the priority passes. The opponent
  watching N identical items resolve is a separate, display-only concern (feature A).

---

## A. Visual stack collapse (client-only)

### A.1 Behaviour

Group **contiguous runs** of identical stack entities into a single rendered pile carrying a `×N`
count badge, expandable on click/hover to the individual cards. Contiguous-only is deliberate: the
stack is LIFO and order is meaningful, so we must never collapse across an intervening
differently-controlled or different-ability item and imply an order that isn't real. (In practice
the high-volume cases — storm copies, simultaneous identical triggers — *are* contiguous because
they're pushed back-to-back.)

Identity key for "identical" (all must match):
- same controller (`controllerId` / `casterId`),
- same source identity (`sourceName`, ideally the source card definition id — see A.3),
- same ability/effect signature (for triggers: `descriptionOverride ?: description`; for spells:
  card name + same `chosenModes` + same X),
- targets ignored for the *summary* line but shown per-item on expand (two copies pointed at
  different creatures still collapse into "Lightning Bolt ×2", expandable to see each target).

### A.2 Rendering

In `StackZone.tsx`, fold the `useStackCards()` list into `StackGroup[]` before laying out:

```ts
type StackGroup = { key: string; items: ClientCard[]; representative: ClientCard }
```

- A group of 1 renders exactly as today.
- A group of N renders one card with a `×N` pip and a slight 3-card "deck" shadow; click toggles an
  expanded fan of the N members. Copy groups already have `copyIndex`/`copyTotal` on the
  `ClientCard`, so storm piles are the cheapest first win.
- Preserve the existing per-card badges (X, kicked, copy index, …) on each expanded member.

No engine, server, or DTO change required for the first cut — everything needed is already on
`ClientCard`.

### A.3 Optional hardening (small server enrichment)

Client-side identity matching on `sourceName` + description strings is heuristic and can mis-group
two genuinely different abilities that happen to share rendered text. If A proves flaky, add a
server-computed `stackGroupKey: String` to the stack DTO (derived from source card definition id +
ability id + controller + mode/X signature) and group on that instead. Defer until needed — keep A
client-only first.

### A.4 Scope / non-goals for A

Display only. Collapsing changes nothing about resolution, priority, or decisions. An opponent's
collapsed pile is purely informational.

---

## B. Batch decision answering ("Yes to all / No to all", "Accept all")

### B.1 Behaviour

When the engine is about to raise the **same decision** for each of N simultaneous, structurally
identical objects, raise it **once** with batch verbs:

- *Yes* / *No* (this one), **Yes to all N** / **No to all N**.

The first and highest-value target is the optional-trigger may-question: a swarm of identical
"whenever … you may …" triggers firing off one event. Secondary target (later): identical simple
target decisions where the player isn't differentiating ("Yes to all" then auto-/self-target).

**Guard (adopted from MTGO `7`):** only batch decisions that are (a) raised by the same ability
identity, (b) fired by the same event / at the same time, and (c) **either targetless or where the
player has opted to apply one answer to all**. Never silently batch anything requiring an ordering
or an individuated, meaningful target choice — that would make a real choice on the player's behalf.

### B.2 Engine design

Today `TriggerProcessor.processTriggers()` walks `liveTriggers` one at a time and
`processMayThenTargetTrigger()` raises a `YesNoDecision` per trigger. Change:

1. **Group before asking.** Before the per-trigger loop, partition `liveTriggers` into runs of
   identical optional triggers (same `controllerId` + same ability identity key — see C.2 — + same
   trigger context shape). A run of length ≥ 2 with no meaningful per-item target becomes one
   `BatchYesNoDecision`.
2. **New decision type** in `PendingDecision.kt`:
   ```kotlin
   data class BatchYesNoDecision(
       override val id: String,
       override val playerId: EntityId,
       val sourceName: String,
       val prompt: String,
       val count: Int,                 // how many identical instances this answer covers
       val yesText: String, val noText: String,
       override val phase: DecisionPhase,
   ) : PendingDecision
   ```
   Answer payload carries `{ applyToAll: Boolean, answer: Boolean }`. `applyToAll=false` peels one
   instance off the run and re-raises the batch for the remaining `count-1` (so "just this one, then
   decide the rest" works); `applyToAll=true` resolves the whole run with that answer.
3. **Continuation.** Reuse the existing `PendingTriggersContinuation` mechanism: a batch "no to all"
   drops the run; "yes to all" enqueues each instance's put-on-stack with the may-gate satisfied.
   Targeted-but-batched instances still fall back to per-instance target selection after a global
   "yes to all" (yes is batched, targeting is not).

### B.3 Server + client

- **Server:** new `BatchYesNoDecision` needs masking + DTO transform alongside the other decision
  types, and a `handleMessage` branch for the batch answer payload (`data-contracts.md` /
  `player-input.md` paths).
- **Client:** new `BatchYesNoDecisionUI` in `web-client/src/components/decisions/`, dispatched from
  `DecisionUI.tsx`. Layout per the bulk-action grammar: the prompt, an `×N` count, and four buttons
  (Yes / No / **Yes to all** / **No to all**).

### B.4 Interaction with auto-pass / "resolve all"

The controller's own resolving stack items already auto-pass (`AutoPassManager` Rule 4), so there's
no separate "resolve all my stack" button to build — B's win is removing the *may-question* spam at
put-on-stack time. A genuine "resolve everything" still cannot be unilateral: each resolution
re-opens a priority window the opponent may use. That is correctly the auto-pass system's job, not
B's; do **not** add an engine path that resolves multiple stack items without re-checking priority.

---

## C. Persistent per-ability yields (MTGO right-click)

### C.1 Behaviour

Let a player set a remembered preference on an ability so it stops prompting:
- **Yield to [ability] until end of turn** — auto-pass priority on this ability's stack objects for
  the rest of the turn.
- **Always yield to [ability]** — same, for the rest of the game.
- **Always answer Yes / Always answer No to [ability]** — auto-resolve its may-question.

Keyed by **ability identity, not stack object** (so it covers every current and future copy/instance,
exactly like MTGO). Must be visible and revocable — a yields panel listing active yields with an
"×" — so a player is never trapped (MTGO's `5` clears all; mirror that with a "Clear yields"
control).

### C.2 The ability identity key (shared with B)

Both B's grouping and C's memory need one stable key for "the same ability." Define:

```
AbilityIdentity = (sourceCardDefinitionId, abilityId)   // NOT entityId, NOT sourceName
```

`abilityId` already exists on triggered/activated abilities (`AbilityId`); the card definition id is
available via `CardRegistry` from the source entity. This key must be threaded onto
`TriggeredAbilityOnStackComponent` (and the activated equivalent) and onto the decision DTO so the
client can offer "always yield to *this*" and the server can match future instances. This is the one
shared SDK/engine surface B and C both depend on — build it once.

### C.3 Storage

A per-player map on `GameState` (immutable, serialized, masked — a player only sees their own
yields):

```kotlin
data class PlayerYields(
    val untilEndOfTurn: Set<AbilityIdentity> = emptySet(),
    val wholeGame: Set<AbilityIdentity> = emptySet(),
    val autoAnswer: Map<AbilityIdentity, Boolean> = emptyMap(),
)
// GameState: yieldsByPlayer: Map<EntityId, PlayerYields>
```

`untilEndOfTurn` is cleared in the cleanup/turn-boundary handler. Because it lives in `GameState`
(not the server session), it survives serialization, replays deterministically, and is naturally
per-player-maskable.

### C.4 Consultation points

- **`AutoPassManager.shouldAutoPass`** gains a check: if the top-of-stack object's `AbilityIdentity`
  is in the priority-holder's `untilEndOfTurn`/`wholeGame` yields → auto-pass. This composes with
  the existing Rule 4 logic; it's an *additional* reason to pass, evaluated for the player who has
  priority.
- **`TriggerProcessor` may-question path** (and B's batch path): before raising the yes/no, if the
  ability's identity has an `autoAnswer` for this player, resolve it without prompting (and skip the
  batch entirely).

### C.5 Server + client

- **Client→server message** to set/clear a yield (right-click / long-press an ability on the stack →
  context menu: *Yield until end of turn* / *Always yield* / *Always yes* / *Always no*). New
  `ClientMessage` + `handleMessage` branch.
- **Client UI:** context menu on stack items (`StackZone.tsx`), plus a small persistent "Active
  yields" chip/panel with per-yield revoke and a clear-all. Surface auto-answered/auto-yielded
  events subtly in the log so the player knows the system acted for them (avoid Arena's "the game
  did something and I don't know what" complaint).

### C.6 Correctness guards

- Yields only ever *auto-pass* or *auto-answer the controller's own may-question*. They must never
  make a targeting, ordering, or modal choice automatically.
- A yield must not auto-pass past a window where the player has a **new** meaningful action that
  wasn't there when they set the yield (lean on `AutoPassManager`'s existing meaningful-action
  filter — yield is one more pass reason layered on top, not a replacement for it).
- Clearing on turn boundary for `untilEndOfTurn` must be in the same place other end-of-turn state
  is reset, to stay replay-deterministic.

---

## 3. Recommended sequencing

1. **A — visual collapse (client-only).** No engine risk, immediate readability win, unblocks
   nothing else. Ship first. Add the optional server `stackGroupKey` (A.3) only if heuristic
   grouping misfires.
2. **C.2 — the `AbilityIdentity` key + threading it onto the stack components and decision DTO.**
   The shared dependency for both B and C; land it as its own change.
3. **B — batch may-question.** Biggest reduction in click-spam for the controller.
4. **C — persistent yields.** The durable, cross-turn version; composes with B's batch path and the
   existing auto-pass.

## 4. Open questions

- **Grouping scope for A:** contiguous-only (safe, proposed) vs all-identical-anywhere (prettier,
  risks implying a false order). Recommend contiguous-only first.
- **Batch peel-off semantics (B):** is "Yes to this one, ask me about the rest" worth the extra
  state, or is Yes-all / No-all / one-at-a-time enough? Lightweight to support via `count-1`
  re-raise; confirm it's wanted before building the UI for it.
- **Yield key granularity (C):** `(cardDefinitionId, abilityId)` treats a card's identical ability
  on two different cards as distinct yields (correct, matches MTGO). Confirm we don't want a
  text-based "any ability that reads X" yield (we don't — too broad, hard to explain).
- **Multiplayer:** all three are per-player and APNAP-agnostic, but A's opponent-facing collapse and
  C's yields-panel need to be checked against the N-player board (`OpponentBoardArea`) so a collapsed
  pile / yield chip attributes to the right seat.

## 5. Build route

B and C add SDK/engine/server/client surface (new decision type, new `GameState` field, new
components, new client→server message), so they go through the **`add-feature`** skill with full
cross-layer tracing and an SDK-reference update for any new decision/serialized type. A is a
self-contained client change. Each phase in §3 is a separately reviewable PR.
