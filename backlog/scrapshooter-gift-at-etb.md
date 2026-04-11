# Scrapshooter (and likely all gift cards) — gift decision at ETB instead of cast time

**File:** `mtg-sets/src/main/kotlin/com/wingedsheep/mtg/sets/definitions/bloomburrow/cards/Scrapshooter.kt`

**Card text ({1}{G}{G} Creature — Raccoon Archer, 4/4):**

> Gift a card *(You may promise an opponent a gift as you cast this spell. If you do, when it
> enters, they draw a card.)*
>
> Reach
>
> When this creature enters, if the gift was promised, destroy target artifact or enchantment
> an opponent controls.

## Current behaviour

Scrapshooter is modeled as a single ETB `ModalEffect.chooseOne`:

- **Mode 1** — "Don't promise a gift" → no-op.
- **Mode 2** — "Promise a gift" → opponent draws a card, then destroy target artifact/enchantment,
  then mark `Effects.GiftGiven()`.

The player chooses between these modes **when the ETB trigger resolves**, not when Scrapshooter
is cast.

## What's broken

### 1. Timing of the gift promise
Per rule 702.171 (Gift), the gift is promised **as the spell is being cast**. That means:

- The promise is locked in *before* the spell goes on the stack.
- Opponents can see the promise when deciding whether to counter the spell.
- If Scrapshooter is countered, the gift is never given (correctly), but the promise still
  happened — which affects replacement effects and cast-triggered cards.
- The controller cannot reactively back out during resolution based on what's currently on the
  battlefield.

Under the current implementation the controller can:

1. Cast Scrapshooter without committing to anything.
2. Watch responses and the board state.
3. On ETB resolution, decide whether to gift based on whether the destroy is worth giving an
   opponent a card.

That is strictly more information than the real card allows.

### 2. The two effects should be separate triggers
Real Scrapshooter has two independent things happening:

- The **gift trigger** (opponent draws a card on ETB, from the gift keyword's reminder text).
- The **destroy trigger** ("When this creature enters, if the gift was promised, destroy target
  artifact or enchantment an opponent controls.")

Both are gated on "was the gift promised", which is a property stored on the spell/permanent
*at cast time*. Bundling them into a single modal means they share resolution ordering, can't be
responded to independently, and can't be reordered between each other (rule 603.3b).

### 3. Targeting is wrong
`Effects.Destroy(EffectTarget.ContextTarget(0))` targets index 0 of the modal's chosen targets.
That's fine *if* mode 2 is chosen. But the destroy target should be chosen at the time the
destroy trigger is placed on the stack (which is ETB, when the gift was promised at cast time),
not at the moment the player opens the modal dialog. The current implementation ties target
selection to mode selection, which is a subtle but observable difference (e.g. the player should
be able to choose the target even before the mode UI appears, because there's no mode — the
gift is already locked in).

## Scope: this is not just Scrapshooter

Every Bloomburrow gift card in `definitions/bloomburrow/cards/` likely uses this same
"modal-at-ETB" workaround. A fix needs to land in the gift framework, not per card.

Cards to audit after fixing the framework (not exhaustive — grep for `Effects.GiftGiven` or
`ModalEffect.chooseOne` on gift-keyword creatures):

- Scrapshooter
- Bakersbane Duo, Daggerfang Duo, Glidedive Duo, Kindlespark Duo, Lifecreed Duo, Lightshell
  Duo, Roughshod Duo, Skyskipper Duo, Treeguard Duo — the "Duo" cycle
- Any other card with "Gift a ..." reminder text

## Fix plan

1. **Add a cast-time gift promise.** When a spell with the `Gift` keyword is cast, present a
   yes/no decision ("Promise a gift to an opponent?") as part of the casting flow, before the
   spell is placed on the stack. Store:
   - Whether the gift was promised (`giftPromised: Boolean`).
   - Which opponent was promised (`giftRecipientId: EntityId?`).
   - What the gift is (card, treasure, food, life — depends on the card; Bloomburrow cards
     always gift "a card draw", but the framework should be generic).

   This is analogous to kicker / additional-cost decisions that already happen at cast time.

2. **Store the promise on the spell/permanent.** New component like
   `GiftPromiseComponent(recipientId, giftType)` attached to the spell on the stack and
   preserved when the permanent enters the battlefield.

3. **Define ETB triggers using `if the gift was promised` as a trigger condition.** The
   existing `triggerCondition` field on `TriggeredAbility` should accept a
   `Conditions.GiftWasPromised` predicate that reads the `GiftPromiseComponent`.

4. **Split Scrapshooter into two triggered abilities:**
   - **Gift draw trigger** (provided by the `Gift` keyword ability — doesn't need to be written
     per-card): `Triggers.EntersBattlefield` + `triggerCondition = GiftWasPromised` +
     `effect = DrawCardsEffect(1, GiftRecipient)`.
   - **Destroy trigger** (on Scrapshooter itself): `Triggers.EntersBattlefield` +
     `triggerCondition = GiftWasPromised` + `target = Targets.ArtifactOrEnchantmentOpponentControls` +
     `effect = Effects.Destroy(ContextTarget(0))`.

5. **Remove the modal wrapper from all gift cards** and let the framework handle the promise.

6. **Remove `Effects.GiftGiven()`** — this current marker effect is a workaround for the
   modal-at-ETB model and would be replaced by the component check.

## Impact

- Gameplay divergence is modest in isolation (the player gets more information than they should
  before committing), but consistent across the entire gift cycle.
- Interactions with cast-time triggers (storm, prowess, "whenever you cast") are likely fine
  because the spell is still cast normally; only the gift bookkeeping changes.
- Interactions with "whenever an opponent draws a card" triggers from *other* cards will fire
  at the right time (ETB) in both the current and fixed model, so those are unaffected.

## Related

- `Effects.GiftGiven()` — marker effect to replace
- `ModalEffect.chooseOne` — currently misused as a pseudo-cast-time decision
- Gift keyword definition (`KeywordAbility.Gift` if it exists; otherwise needs to be added)
- Cast-time decision plumbing — see how Kicker / additional cost decisions are collected during
  `CastSpellHandler` for precedent
