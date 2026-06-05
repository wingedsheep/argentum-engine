# Scoping: unifying `PayCost` payment (the "`PayCost → Effect`" question)

**Status:** Option C in progress — PR 1 (`CostPaymentService` + continuation) and **PR 2 (migrate
Morph)** landed. **Owner:** TBD. **Related:**
[`gated-effect-migration-handoff-remaining.md`](gated-effect-migration-handoff-remaining.md)
(this doc closes out the open question its §5 #4 left — "should `PayOrSufferEffect` fold onto the
gated frame via a `PayCost → Effect` adapter?").

> **TL;DR.** The literal "`PayCost → Effect`" unification — turning every `PayCost` variant into a
> resolution `Effect` so gate frames can consume it — is the **wrong axis**. It conflates two
> distinct MTG concepts (a *cost* you pay vs. an *effect* that happens), and it wouldn't remove the
> machinery it appears to. The real, high-value refactor hiding behind the question is a **single
> shared cost-payment engine**: today **five** consumers each carry their own `when (cost: PayCost)`
> switch, each covering a *different, incomplete* subset of the 10 variants. Consolidating those into
> one `CostPaymentService` is the elegant move, is orthogonal to the gated frame, and *incidentally*
> makes folding `PayOrSufferEffect` (and `AnyPlayerMayPayEffect`) trivial afterward. Recommendation:
> **Option C** below. Estimated 1 medium PR for the service + 1 PR per migrated consumer.

---

## 1. Why this came up

While folding the gated-effect wrappers onto `GatedEffect` (handoff §5), `#4 PayOrSufferEffect` was
the one wrapper that wouldn't fold cleanly. `Gate.MayPay` carries a cost **`Effect`**
(`PayManaCostEffect`, `PayLifeEffect`, `SacrificeEffect`, or a `CompositeEffect` of them);
`PayOrSufferEffect` carries a cost **`PayCost`** — a different, richer hierarchy. The note left in the
handoff was: *"the only clean shape would be a dedicated `PayCost → Effect` adapter."* This doc scopes
that idea and finds a better target.

## 2. The `PayCost` hierarchy (10 variants)

`mtg-sdk/src/main/kotlin/com/wingedsheep/sdk/scripting/costs/PayCost.kt` — a `@Serializable sealed
interface PayCost : TextReplaceable<PayCost>` with a `description`:

| Variant | Shape | Payment UX |
|---|---|---|
| `Mana(cost: ManaCost)` | pay a fixed mana cost | mana-source selection / auto-tap |
| `OwnManaCost` (object) | pay the affected permanent's *own* printed mana cost | resolved to `Mana` at payment time |
| `Discard(filter, count, random)` | discard N matching cards from hand | card-selection (or random) |
| `Sacrifice(filter, count)` | sacrifice N matching permanents you control | battlefield targeting |
| `PayLife(amount)` | pay N life | yes/no |
| `Exile(filter, zone, count)` | exile N matching cards from a zone | card-selection |
| `RevealCard(filter, count)` | reveal N matching cards from hand (they stay) | card-selection |
| `Choice(options: List<PayCost>)` | pick one of several sub-costs to pay | option pick → recurse |
| `ReturnToHand(filter, count)` | return N permanents you control to hand | battlefield targeting |
| `Tap(filter, count)` | tap N untapped permanents you control | battlefield targeting |

`PayCost` is a genuine, useful abstraction: a **payable, all-or-nothing, affordability-checkable**
cost that exists outside the stack. That is exactly what MTG calls a *cost* (CR 118) and deliberately
*not* an effect.

## 3. The actual problem: five divergent payment switches

`PayCost` is **not** a `PayOrSuffer`-only concern. Five consumers hold a `PayCost`, and each
reimplements payment in its own `when (cost)` with a **different, incomplete** variant subset:

**Consumers (who holds a `PayCost`):**

| Holder | File | Use |
|---|---|---|
| `KeywordAbility.Morph.morphCost` | `mtg-sdk/.../scripting/KeywordAbility.kt` | morph face-up cost ("Morph {3}{G}{G}" / "Morph—Pay 5 life") |
| `PayOrSufferEffect.cost` | `mtg-sdk/.../scripting/effects/CompositeEffects.kt` | punisher "[suffer] unless you [cost]" |
| `AnyPlayerMayPayEffect.cost` | `mtg-sdk/.../scripting/effects/CompositeEffects.kt` | "any player may [cost]" (APNAP) |
| `ChainCopyEffect.copyCost` | `mtg-sdk/.../scripting/effects/ChainCopyEffects.kt` | pay-to-copy chains |
| `CardBuilder.morphCost` | `mtg-sdk/.../dsl/CardBuilder.kt` | DSL for non-mana morph costs |
| `MorphDataComponent.morphCost` | `rules-engine/.../state/components/identity/FaceDownComponents.kt` | runtime storage |

**Payment implementations (who switches on `PayCost`), and the coverage gaps:**

| Variant | Morph `TurnFaceUpHandler` | `PayOrSufferExecutor` | `AnyPlayerMayPayExecutor` | `ChainCopyExecutor` |
|---|:---:|:---:|:---:|:---:|
| `Mana` | ✅ | ✅ | ❌ | ❌ |
| `OwnManaCost` | ❌ *"not supported"* | ✅ | ❌ | ❌ |
| `Discard` | ✅ | ✅ | ❌ | ✅ |
| `Sacrifice` | ✅ | ✅ | ✅ | ✅ |
| `PayLife` | ✅ | ✅ | ✅ | ❌ |
| `Exile` | ✅ | ✅ | ❌ | ❌ |
| `RevealCard` | ✅ | ❌ *"not implemented"* | ❌ | ❌ |
| `Choice` | ❌ *"not supported"* | ✅ | ❌ | ❌ |
| `ReturnToHand` | ✅ | ❌ *"not implemented"* | ❌ | ❌ |
| `Tap` | ❌ *"not supported"* | ✅ | ❌ | ❌ |

Plus a **sixth** switch in `legalactions/enumerators/TurnFaceUpEnumerator.kt` (affordability for the
face-up legal action), and affordability re-implemented again inside `PayOrSufferExecutor.canPayCost`
and `AnyPlayerMayPayExecutor.canPlayerPay`.

This is the same "enumeration duplicated N times, each subtly different" smell the gated-effect lesson
attacked — but one layer down, at *payment* rather than *gating*. The concrete cost of the
duplication: a card author who writes `PayCost.Tap` as a morph cost gets a runtime
*"Tap morph costs are not supported"*; `PayCost.ReturnToHand` as a punisher cost gets *"not yet
implemented"*. The variant works in one consumer and silently fails in another.

## 4. The `Effect`-side payment primitives (what exists, what's missing)

For the literal "`PayCost → Effect`" idea to work, every variant needs an `Effect` that performs the
same action. Inventory:

| `PayCost` variant | Equivalent `Effect`? |
|---|---|
| `Mana` | ✅ `PayManaCostEffect` (`ManaEffects.kt`) |
| `OwnManaCost` | ⚠️ none — resolves to `Mana` at payment time by reading the source's `CardComponent.manaCost` (needs the source entity, not expressible as a static `Effect`) |
| `PayLife` | ✅ `PayLifeEffect` (`LifeEffects.kt`) |
| `Sacrifice` | ✅ `SacrificeEffect` / `SacrificeSelfEffect` / `ForceSacrificeEffect` (`RemovalEffects.kt`) |
| `Tap` | ✅ `TapUntapEffect(tap = true)` (`TapEffects.kt`) — but targets, doesn't *select-to-pay* |
| `ReturnToHand` | ◑ `ForceReturnOwnPermanentEffect` / generic `MoveToZoneEffect(HAND)` |
| `Discard` | ❌ **no standalone discard-as-cost effect** (only composed forms like `DrawRevealDiscardUnlessEffect`) |
| `Exile` | ❌ **no "exile N from zone X matching filter" effect** (only generic `MoveToZoneEffect(EXILE)`) |
| `RevealCard` | ◑ only `MayRevealCardFromHandEffect` (optional) — no mandatory reveal-to-pay |
| `Choice` | ❌ not an effect — resolved as a `ChooseOptionDecision` inside the executor |

So a literal unification needs at least **3 new `Effect` types** (`DiscardEffect`,
`ExileFromZoneEffect`, mandatory `RevealCardsEffect`), a non-trivial `OwnManaCost` special case, and a
cost-flavored `Choice` — *and* a way to express "affordability" and "was it actually paid" uniformly
across effects.

## 5. Cost vs. effect — why the literal unification is the wrong axis

MTG (and this engine) treats a **cost** and an **effect** as different things:

- A **cost** is paid (CR 118): it's checked for affordability *before* you commit ("you can't pay a
  cost you can't pay"), it's atomic/all-or-nothing, it doesn't use the stack, and it can't be
  responded to. `PayCost` models this — and the engine *uses* the cost shape: `TurnFaceUpEnumerator`
  asks "can the player afford this `PayCost`?" to decide whether the face-up action is even legal.
- An **effect** happens during resolution and just runs; "did it accomplish anything" is a *post-hoc*
  question (that's literally what `Gate.DoAction` + `SuccessCriterion` had to invent).

`Gate.MayPay` already shows the seam. On "yes" it runs `CompositeEffect([cost, then], stopOnError =
true)` and infers payment from whether the cost effect *errored*. That works for mana/life (where
`canAfford` pre-checks and the executor errors on shortfall) but is fragile for selection costs: a
`SacrificeEffect` that the player *declines* or can only *partially* pay doesn't cleanly signal
"unpaid" — there's no first-class "payment failed, refund and take the other branch" protocol. The
`PayOrSufferExecutor`, by contrast, does this correctly per variant (feasibility pre-check →
auto-suffer with no prompt; exact-count selection → pay or fall through).

**Conclusion:** collapsing `PayCost` into `Effect` would either (a) lose the affordability /
all-or-nothing semantics that morph and punisher costs depend on, or (b) re-introduce them as an
"is this effect a fully-paid cost?" protocol bolted onto `Effect` — i.e. rebuild `PayCost`'s
semantics inside `Effect`. Either way it doesn't simplify; it relocates and blurs.

## 6. Options

### Option A — Full `PayCost → Effect` replacement (the literal ask). ❌ Not recommended.
Delete `PayCost`; express every cost as an `Effect`; add the 3 missing effect types + an
affordability/"paid" protocol on `Effect`; rewrite morph, chain-copy, any-player, punisher to consume
cost effects. **Blast radius:** all 5 consumers incl. deep morph surgery, 35 card definitions, the
face-up enumerator, `MorphDataComponent` serialization. **Verdict:** highest cost, conflates
cost/effect, and the new affordability protocol re-creates `PayCost` semantics. Reject.

### Option B — One-directional `PayCost.toEffect()` adapter. ❌ Not recommended.
Keep `PayCost` as authoring vocabulary; add an adapter so `Gate.MayPay` can consume a `PayCost` by
lowering it to a cost `Effect`, letting `PayOrSufferEffect` fold to `GatedEffect(gate =
MayPay(cost.toEffect()), then = noop, otherwise = suffer)`. **Problem:** the adapter must reproduce
per-variant affordability + the rich selection UX (battlefield targeting for sacrifice/tap,
card-selection for discard/exile, yes/no for mana/life, recursive `Choice`) — i.e. the adapter *is*
most of `PayOrSufferExecutor`, now living behind a lossy `Effect` façade. Net LOC ≈ unchanged, clarity
worse. Reject.

### Option C — One shared `CostPaymentService`. ✅ Recommended.
Extract the payment machinery into a single engine service that owns, for all 10 variants, in one
place:
- `canAfford(state, payer, cost, source): Boolean` — the affordability pre-check (replaces the 3+
  duplicated `canPayCost`/`canPlayerPay`/enumerator checks).
- `pay(state, payer, cost, source, context): PaymentResult` — performs the payment, pausing with the
  right decision (battlefield targeting / card-selection / yes/no / option pick) and resuming via a
  **single** `CostPaymentContinuation`, returning `Paid` / `Declined` / `Unaffordable` (+ events).

All five consumers become thin callers:
- **Morph** `TurnFaceUpHandler` / `TurnFaceUpEnumerator` → delegate (gains Choice/Tap/OwnManaCost for
  free; deletes ~250 lines of `payMorphCost_*`).
- **`PayOrSufferExecutor`** → `if (!canAfford) suffer else pay(...).onDeclined { suffer }` (gains
  ReturnToHand/RevealCard; the executor shrinks to a few lines).
- **`AnyPlayerMayPayExecutor`** → loop APNAP calling `canAfford`/`pay` (gains all 10 variants).
- **`ChainCopyExecutor`** → delegate (gains all variants).

**This is the real consolidation** and it's *orthogonal to the gated frame*. It removes 5 divergent
switches + 3 affordability copies, erases every "not supported"/"not implemented" gap at once, and
gives one place to fix payment bugs. No new `Effect` types, no cost/effect blurring, `PayCost` stays
the clean authoring vocabulary.

### Option D — Do nothing; keep the boundary documented. ◑ Acceptable fallback.
The gated migration already left #4/#6 bespoke with documented reasons. If the duplication isn't
hurting, stop here. (But the live "not supported" runtime errors argue for Option C.)

## 7. How Option C closes the gated-effect question

Once `CostPaymentService` exists, folding the two remaining gated-effect wrappers becomes a *small*
follow-up, if still desired — and on the **right** axis (a gate that carries a `PayCost` and delegates
to the service, not a cost masquerading as an `Effect`):

- **`PayOrSufferEffect`** → optionally a `Gate.PayCostOrSuffer(cost: PayCost)` whose executor branch
  calls `CostPaymentService` (`then = ∅`, `otherwise = suffer`). Only worth doing if it deletes code
  net of the new branch; the *real* win (dedup) already landed with the service.
- **`AnyPlayerMayPayEffect`** stays its own effect (multi-player APNAP loop), but its executor body
  becomes a thin `CostPaymentService` loop.

So the honest answer to "scope the `PayCost → Effect` unification": **don't unify the types — unify
the payment machinery.** That delivers the elegance the question was reaching for, without the
cost/effect category error.

## 8. Suggested phasing

1. **PR 1 — `CostPaymentService` + `CostPaymentContinuation`** with full 10-variant `canAfford` + `pay`
   (lift the most-complete logic, which lives across `PayOrSufferExecutor` (8) and `TurnFaceUpHandler`
   (7), into one place). Unit-test every variant's affordability + pay/decline. No consumer migrated
   yet (service unused → no behavior change to verify against).
2. **PR 2 — migrate Morph** (`TurnFaceUpHandler` + `TurnFaceUpEnumerator`) onto the service. Biggest
   single dedup; unlocks Choice/Tap/OwnManaCost morph costs. Existing morph scenario tests are the
   safety net; add tests for the newly-enabled variants. — **DONE.** Non-mana morph costs delegate to
   `CostPaymentService.pay(onPaid = TurnFaceUpEffect(Self))`; completion rides on `onPaid`, so a
   declined/unaffordable cost simply leaves the creature face down. `canAfford` was lifted to a
   `CostPaymentService` companion (parameterized by `ManaSolver`) so the enumerator shares the one
   affordability impl. **Mana morph payment deliberately stays in the handler** — the service's yes/no
   mana path doesn't model morph's explicit mana-source selection / X / auto-tap preview, so migrating
   it would be a UX + capability regression. The per-variant payment switch, the `validate()` target
   checks, the `find*Targets` helpers, and the enumerator's per-variant affordability all collapsed;
   also fixed a CR 119.4 bug (pay-life morph was offered below the required life).
3. **PR 3 — migrate `PayOrSufferExecutor`** onto the service (gains ReturnToHand/RevealCard).
4. **PR 4 — migrate `AnyPlayerMayPayExecutor` + `ChainCopyExecutor`** onto the service (each gains all
   variants).
5. **PR 5 (optional) — gated-frame fold**: `Gate.PayCostOrSuffer` for `PayOrSufferEffect`, only if it
   nets out smaller. Closes handoff §5 #4.

Each PR is independently shippable and snapshot-neutral (these are engine-execution changes, not SDK
shape changes — *except* PR 5, which would be a `PayOrSuffer` → `Gated` snapshot rename).

## 9. Risks / watch-outs

- **Decision-UX parity.** `PayOrSuffer` uses on-battlefield targeting for sacrifice/tap and
  card-selection overlays for discard/exile; morph uses its own prompts. The service must preserve the
  *better* UX per variant (battlefield selection over overlays — see the UX-review feedback), and each
  migration PR must trace the player flow end-to-end, not just assert engine state.
- **Affordability must stay a pre-check for legal actions.** `TurnFaceUpEnumerator` needs
  `canAfford` to gate the face-up *action* (you can't even start it if unaffordable). Keep `canAfford`
  pure/allocation-light — it runs in legal-action enumeration (hot path).
- **`OwnManaCost` needs the source entity** at payment time; the service signature must thread
  `source` (it already does in every current call site).
- **Partial-payment / decline semantics** must be explicit (`PaymentResult.Declined` vs
  `Unaffordable` vs `Paid`) so callers branch correctly — this is the bug-prone seam today.
- **`Choice` recursion + APNAP**: the service's continuation must handle a `Choice` option that
  itself pauses, and `AnyPlayerMayPay` asking each player in turn — i.e. the continuation carries
  "which player, which (sub)cost, remaining players."

## 10. Decision needed from owner

Pick **C** (recommended: build the shared service, migrate consumers, optionally fold #4 last) or
**D** (document the boundary and stop). A/B are scoped here only to record *why* the literal
"`PayCost → Effect`" framing is rejected.
