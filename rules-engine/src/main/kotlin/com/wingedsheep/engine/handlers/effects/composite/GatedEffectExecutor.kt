package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.PayDynamicManaCostEffect
import com.wingedsheep.sdk.scripting.effects.PayLifeEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import java.util.UUID
import kotlin.reflect.KClass

/**
 * One executor for the [GatedEffect] frame — the unified replacement for the bespoke
 * may / optional-cost executors. It owns the canonical resolution order for every
 * decision-driven [Gate]:
 *
 *  1. Resolve the decision-maker (defaults to the ability's controller).
 *  2. [Gate.MayPay] only: skip the prompt entirely when the cost is unaffordable, falling
 *     through to [GatedEffect.otherwise] — asking yes/no for an unpayable cost is a UX trap
 *     that can also silently drop pieces of a composite cost.
 *  3. Pause with a [YesNoDecision]. The [GatedEffectContinuation] carries the *already-locked*
 *     targets in its [EffectContext], so a targeted [GatedEffect.then] resolves against the
 *     trigger-time target (CR 603.3d) instead of re-choosing one at resolution.
 *
 * The "yes" branch is consumed by `EffectAndTriggerContinuationResumer.resumeGatedEffect`;
 * the gate kind decides *how* — run [GatedEffect.then] directly ([Gate.MayDecide]) or pay the
 * cost first ([Gate.MayPay]).
 */
class GatedEffectExecutor(
    private val cardRegistry: CardRegistry,
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<GatedEffect> {

    override val effectType: KClass<GatedEffect> = GatedEffect::class

    private val manaSolver = ManaSolver(cardRegistry)
    private val conditionEvaluator = ConditionEvaluator()
    private val dynamicAmountEvaluator = DynamicAmountEvaluator()

    override fun execute(
        state: GameState,
        effect: GatedEffect,
        context: EffectContext
    ): EffectResult {
        val gate = effect.gate

        // Gate.WhenCondition: a synchronous state test, not a decision — no prompt, no pause.
        // Evaluate through the same ConditionEvaluationContext used everywhere else (so it reads
        // identically at resolution and under projection) and run `then` / `otherwise` directly.
        if (gate is Gate.WhenCondition) {
            val otherwise = effect.otherwise
            return when {
                conditionEvaluator.evaluate(state, gate.condition, context) ->
                    effectExecutor(state, effect.then, context)
                otherwise != null -> effectExecutor(state, otherwise, context)
                else -> EffectResult.success(state)
            }
        }

        // Gate.DoAction: an action-outcome gate, not a decision. Run `action` (which may pause for
        // its own sub-decisions); once it has fully drained, score it via the SuccessCriterion
        // against a pre-action snapshot to pick `then` (it happened) vs `otherwise` (it didn't).
        if (gate is Gate.DoAction) {
            return executeDoAction(state, gate, effect, context)
        }

        // Gate.MayPayX: a number-chooser pay-gate, not a yes/no — prompt 0..max affordable generic
        // mana, pay the chosen X, and run `then` with X bound into the context. Reuses the existing
        // MayPayXContinuation / resumeMayPayX machinery (only the type recognition moved onto the frame).
        if (gate is Gate.MayPayX) {
            return executeMayPayX(state, effect, context)
        }

        // Gate.MayDecide: two cases where the former MayEffect skipped the prompt entirely.
        if (gate is Gate.MayDecide) {
            // Source must still be in its required zone (e.g. a dies-trigger "may" whose source
            // has since left) — otherwise the may-action is impossible, so skip silently.
            if (gate.sourceRequiredZone != null && context.sourceId != null) {
                val inRequiredZone = state.zones.any { (zoneKey, entities) ->
                    zoneKey.zoneType == gate.sourceRequiredZone && context.sourceId in entities
                }
                if (!inRequiredZone) return EffectResult.success(state)
            }
            // A ChooseActionEffect payoff with no feasible choice — don't ask the may question at all.
            val then = effect.then
            if (then is ChooseActionEffect &&
                then.choices.none { checkFeasibility(state, context.controllerId, it.feasibilityCheck) }
            ) {
                return EffectResult.success(state)
            }
        }

        val playerId = effect.decisionMaker
            ?.let { TargetResolutionUtils.resolvePlayerTarget(it, context, state) }
            ?: context.controllerId

        // Gate.MayPay: don't offer an impossible "yes" — fall straight through to `otherwise`.
        if (gate is Gate.MayPay && !canAfford(state, playerId, gate.cost, context)) {
            return effect.otherwise
                ?.let { effectExecutor(state, it, context) }
                ?: EffectResult.success(state)
        }

        // An optional *mana* payment (the lowered MayPayManaEffect shape) keeps its bespoke UX —
        // a "Pay {cost}?" yes/no that, on "yes", routes through the mana-source-selection
        // continuations rather than the generic auto-tapping cost composite. See [OptionalManaPayment].
        effect.asOptionalManaPayment()?.let { mana ->
            return executeOptionalManaPayment(state, playerId, mana.cost, mana.then, context)
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val hint = when (gate) {
            is Gate.MayDecide -> gate.hint ?: effect.hint
            is Gate.MayPay -> effect.hint
            is Gate.WhenCondition -> effect.hint // unreachable: handled by the synchronous branch above
            is Gate.DoAction -> effect.hint // unreachable: handled by the action-drain branch above
            is Gate.MayPayX -> effect.hint // unreachable: handled by the number-chooser branch above
        }

        // For a pay-gate, label the "yes" button with the concrete cost — a dynamic cost
        // ("pay {4} for each chosen creature") renders its *computed* total ("Pay {8}") so the
        // player confirms a number, not a formula.
        val payLabel = (gate as? Gate.MayPay)?.let { computedCostLabel(state, it.cost, context) }

        val decisionId = UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = effect.description,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION,
                triggeringEntityId = context.triggeringEntityId,
                inlineOnTrigger = (gate as? Gate.MayDecide)?.inlineOnTrigger ?: false
            ),
            yesText = payLabel ?: "Yes",
            noText = if (payLabel != null) "Don't pay" else "No",
            hint = hint
        )

        val continuation = GatedEffectContinuation(
            decisionId = decisionId,
            gate = gate,
            then = effect.then,
            otherwise = effect.otherwise,
            effectContext = context
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * The optional-mana-payment yes/no — formerly `MayPayManaExecutor`. Affordability is already
     * checked by the [canAfford] pre-pass above, so this only builds the "Pay {cost}?" prompt and a
     * [MayPayManaContinuation]; the existing mana-payment resumer then either spends floating mana or
     * pauses for manual mana-source selection before running [then].
     */
    private fun executeOptionalManaPayment(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        then: Effect,
        context: EffectContext
    ): EffectResult {
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay $manaCost?",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION,
                triggeringEntityId = context.triggeringEntityId
            ),
            yesText = "Pay $manaCost",
            noText = "Don't pay"
        )

        val continuation = MayPayManaContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceName = sourceName,
            manaCost = manaCost,
            effect = then,
            effectContext = context
        )

        val stateWithContinuation = state.withPendingDecision(decision).pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resolve a [Gate.MayPayX] gate (the lowered `MayPayXForEffect`). Computes the most generic mana
     * the decision-maker can produce and, if any, pauses with a 0..max number chooser; the existing
     * [MayPayXContinuation] resumer (`resumeMayPayX`) then auto-taps the chosen X and runs
     * [GatedEffect.then] with `xValue` bound into the context. An unaffordable gate (max <= 0) falls
     * through to [GatedEffect.otherwise] (or nothing), mirroring the former executor's silent skip.
     */
    private fun executeMayPayX(
        state: GameState,
        effect: GatedEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = effect.decisionMaker
            ?.let { TargetResolutionUtils.resolvePlayerTarget(it, context, state) }
            ?: context.controllerId

        val maxAffordable = manaSolver.getAvailableManaCount(state, playerId)
        if (maxAffordable <= 0) {
            return effect.otherwise
                ?.let { effectExecutor(state, it, context) }
                ?: EffectResult.success(state)
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseNumberDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay {X}? Choose X (0 to decline)",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION,
                triggeringEntityId = context.triggeringEntityId
            ),
            minValue = 0,
            maxValue = maxAffordable
        )

        val continuation = MayPayXContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceName = sourceName,
            effect = effect.then,
            maxX = maxAffordable,
            effectContext = context
        )

        val stateWithContinuation = state.withPendingDecision(decision).pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "CHOOSE_NUMBER",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Whether [playerId] can pay [cost] right now. Mirrors the former
     * `OptionalCostEffectExecutor`: recognizes the payment primitives that appear in a
     * [Gate.MayPay] cost slot ([PayManaCostEffect], [PayDynamicManaCostEffect], [PayLifeEffect], and
     * a [CompositeEffect] composing them). The dynamic-mana branch charges the cost's own `payer`, so
     * affordability stays correct even when it differs from the gate's decisionMaker. Unknown shapes
     * fail open (assumed payable) so exotic cost pipelines still prompt and abort later via the
     * resumer's `stopOnError` composite.
     */
    /**
     * Render a [Gate.MayPay] cost as a concrete "Pay …" button label, or null for shapes with no
     * single obvious rendering (life, sacrifice, composites) — those keep the plain "Yes". A
     * [PayDynamicManaCostEffect] shows its resolution-computed total, not the formula.
     */
    private fun computedCostLabel(state: GameState, cost: Effect, context: EffectContext): String? =
        when (cost) {
            is PayManaCostEffect -> "Pay ${cost.cost}"
            is PayDynamicManaCostEffect -> {
                val amount = dynamicAmountEvaluator.evaluate(state, cost.amount, context)
                "Pay {$amount}"
            }
            else -> null
        }

    private fun canAfford(state: GameState, playerId: EntityId, cost: Effect, context: EffectContext): Boolean =
        when (cost) {
            is PayManaCostEffect -> manaSolver.canPay(state, playerId, cost.cost)
            is PayDynamicManaCostEffect -> {
                // Affordability must target whoever actually foots the bill — resolve the cost's own
                // `payer` rather than trusting the gate's decisionMaker to match it. A computed
                // amount of <= 0 is free.
                val amount = dynamicAmountEvaluator.evaluate(state, cost.amount, context)
                val payerId = TargetResolutionUtils
                    .resolvePlayerTarget(EffectTarget.PlayerRef(cost.payer), context, state)
                    ?: playerId
                amount <= 0 || manaSolver.canPay(state, payerId, ManaCost.parse("{$amount}"))
            }
            is PayLifeEffect -> {
                val life = state.getEntity(playerId)?.get<LifeTotalComponent>()?.life ?: 0
                life >= cost.amount
            }
            is CompositeEffect -> cost.effects.all { canAfford(state, playerId, it, context) }
            else -> true
        }

    /**
     * Resolve a [Gate.DoAction] gate (the lowered `IfYouDoEffect`). Follows the former
     * `IfYouDoEffectExecutor`'s pre-push pattern: a [GatedActionContinuation] is pushed *before*
     * the action runs. If the action completes synchronously the continuation is popped inline and
     * the outcome evaluated; otherwise it stays on the stack for the auto-resumer
     * (`CoreAutoResumerModule`) to pick up once the action's own continuations have drained.
     */
    private fun executeDoAction(
        state: GameState,
        gate: Gate.DoAction,
        effect: GatedEffect,
        context: EffectContext
    ): EffectResult {
        val snapshot = captureSnapshot(state, gate.action, gate.successCriterion, context)

        val continuation = GatedActionContinuation(
            decisionId = "pending",
            then = effect.then,
            otherwise = effect.otherwise,
            successCriterion = gate.successCriterion,
            snapshot = snapshot,
            effectContext = context
        )
        val stateWithCont = state.pushContinuation(continuation)

        val result = effectExecutor(stateWithCont, gate.action, context)

        if (result.isPaused) {
            // Action paused; leave the continuation on the stack for the auto-resumer.
            return result
        }

        // Action ran synchronously (success or recoverable error). Pop our pre-pushed
        // continuation and evaluate the outcome inline.
        val (_, stateWithoutCont) = result.state.popContinuation()
        return evaluateAndDispatch(
            state = stateWithoutCont,
            then = effect.then,
            otherwise = effect.otherwise,
            criterion = gate.successCriterion,
            snapshot = snapshot,
            effectContext = context,
            priorEvents = result.events,
            effectExecutor = effectExecutor
        )
    }

    /**
     * Capture the data the [SuccessCriterion] needs to evaluate the post-action delta.
     *
     * For [SuccessCriterion.Auto], the probe recognizes two zone-move shapes and records the
     * destination zone's pre-execution size:
     * - a terminal pipeline [MoveCollectionEffect] (multi-object moves), and
     * - a terminal single-target [MoveToZoneEffect] whose target is [EffectTarget.Self]
     *   (e.g. "exile this card from your graveyard. If you do, …" — Council's Deliberation).
     *
     * The collection move is checked first so a pipeline that ends in one keeps its existing
     * semantics. Single-target moves with a non-Self target aren't resolvable here without full
     * target resolution, so they (and genuinely non-move actions such as deal-damage / gain-life)
     * yield an empty snapshot and fall through to the criterion's intrinsic evaluation — for
     * [SuccessCriterion.Auto] that means [evaluateAuto]'s fail-open default, which is correct for
     * actions whose "did it happen" isn't a zone delta.
     */
    private fun captureSnapshot(
        state: GameState,
        action: Effect,
        criterion: SuccessCriterion,
        context: EffectContext
    ): GatedActionSnapshot {
        if (criterion !is SuccessCriterion.Auto) return GatedActionSnapshot()

        findTerminalMove(action)?.let { move ->
            val destination = move.destination as? CardDestination.ToZone ?: return GatedActionSnapshot()
            val ownerId = resolvePlayer(destination.player, context) ?: return GatedActionSnapshot()
            return zoneSnapshot(state, ownerId, destination.zone)
        }

        findTerminalSingleMove(action)?.let { move ->
            // Only the Self target resolves to a concrete moved entity here; the destination
            // zone is owned by that entity's owner (e.g. self-exile from a graveyard lands in
            // that card's owner's exile). Other targets fall through to fail-open as before.
            if (move.target !is EffectTarget.Self) return GatedActionSnapshot()
            val movedId = context.sourceId ?: return GatedActionSnapshot()
            val ownerId = state.getEntity(movedId)?.get<OwnerComponent>()?.playerId ?: return GatedActionSnapshot()
            return zoneSnapshot(state, ownerId, move.destination)
        }

        return GatedActionSnapshot()
    }

    private fun zoneSnapshot(state: GameState, ownerId: EntityId, zone: Zone): GatedActionSnapshot =
        GatedActionSnapshot(
            destinationZoneOwner = ownerId,
            destinationZoneType = zone,
            destinationZonePreSize = state.zones[ZoneKey(ownerId, zone)]?.size ?: 0
        )

    /**
     * Walk the effect tree for the last [MoveCollectionEffect] in execution order.
     * Returns null for shapes the auto-probe doesn't recognize.
     */
    private fun findTerminalMove(effect: Effect): MoveCollectionEffect? = when (effect) {
        is MoveCollectionEffect -> effect
        is CompositeEffect -> effect.effects.asReversed().firstNotNullOfOrNull { findTerminalMove(it) }
        else -> null
    }

    /**
     * Walk the effect tree for the last single-target [MoveToZoneEffect] in execution order.
     * Returns null for shapes the auto-probe doesn't recognize.
     */
    private fun findTerminalSingleMove(effect: Effect): MoveToZoneEffect? = when (effect) {
        is MoveToZoneEffect -> effect
        is CompositeEffect -> effect.effects.asReversed().firstNotNullOfOrNull { findTerminalSingleMove(it) }
        else -> null
    }

    private fun resolvePlayer(player: Player, context: EffectContext): EntityId? = when (player) {
        is Player.You -> context.controllerId
        is Player.Opponent -> context.opponentId
        is Player.TargetOpponent -> context.opponentId
        is Player.TargetPlayer -> context.targets.firstOrNull()?.let { TargetResolutionUtils.run { it.toEntityId() } }
        is Player.ContextPlayer -> context.positionalTarget(player.index)?.let { TargetResolutionUtils.run { it.toEntityId() } }
        is Player.TriggeringPlayer -> context.triggeringEntityId
        else -> context.controllerId
    }

    companion object {
        /**
         * Evaluate a [Gate.DoAction] criterion against the post-action state and dispatch `then`
         * (it happened) or `otherwise` (it didn't). Shared between the synchronous path in
         * [executeDoAction] and the auto-resumer that handles paused-action completion.
         */
        fun evaluateAndDispatch(
            state: GameState,
            then: Effect,
            otherwise: Effect?,
            criterion: SuccessCriterion,
            snapshot: GatedActionSnapshot,
            effectContext: EffectContext,
            priorEvents: List<GameEvent>,
            effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
        ): EffectResult {
            val happened = evaluate(state, criterion, snapshot)
            val branch = if (happened) then else otherwise
                ?: return EffectResult.success(state, priorEvents)
            val branchResult = effectExecutor(state, branch, effectContext)
            return branchResult.copy(events = priorEvents + branchResult.events)
        }

        /**
         * Did the action accomplish its work, given the snapshot taken before it ran?
         */
        private fun evaluate(
            state: GameState,
            criterion: SuccessCriterion,
            snapshot: GatedActionSnapshot
        ): Boolean = when (criterion) {
            is SuccessCriterion.Always -> true
            is SuccessCriterion.Auto -> evaluateAuto(state, snapshot)
            is SuccessCriterion.CollectionNonEmpty ->
                // Pipeline storage doesn't propagate up to this level after resume — until that
                // plumbing exists, fall back to Auto's zone-delta probe (set by captureSnapshot
                // when the action's terminal move is recognized).
                evaluateAuto(state, snapshot)
        }

        private fun evaluateAuto(state: GameState, snapshot: GatedActionSnapshot): Boolean {
            val owner = snapshot.destinationZoneOwner
            val zone = snapshot.destinationZoneType
            if (owner == null || zone == null) {
                // No zone-move probe was capturable. This is reached only for non-zone-move actions
                // (deal damage, gain/lose life, draw, …) whose "did it happen" isn't a zone-size
                // delta — for those, treating the action as performed (fail open) is the correct
                // default. Zone-move shapes (collection moves and self-target single moves) are
                // probed in captureSnapshot and never land here.
                return true
            }
            val postSize = state.zones[ZoneKey(owner, zone)]?.size ?: 0
            return postSize > snapshot.destinationZonePreSize
        }
    }
}
