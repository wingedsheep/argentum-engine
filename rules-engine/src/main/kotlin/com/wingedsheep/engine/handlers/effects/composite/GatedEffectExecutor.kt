package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver
import com.wingedsheep.engine.legalactions.utils.CostEnumerationUtils
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.TapForGeneric
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TriggeredAbilityEffectAppliedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.CollectEvidenceEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.DynamicHint
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.PayDynamicLifeEffect
import com.wingedsheep.sdk.scripting.effects.PayDynamicManaCostEffect
import com.wingedsheep.sdk.scripting.effects.PayExactCountersEffect
import com.wingedsheep.sdk.scripting.effects.PayLifeEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostEffect
import com.wingedsheep.sdk.scripting.effects.PayManaCostRepeatedlyEffect
import com.wingedsheep.sdk.scripting.effects.DamageRecipient
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val predicateEvaluator: PredicateEvaluator
) : EffectExecutor<GatedEffect> {
    private val dynamicAmountEvaluator = predicateEvaluator.amounts
    private val conditionEvaluator = predicateEvaluator.conditions

    override val effectType: KClass<GatedEffect> = GatedEffect::class

    private val manaSolver = ManaSolver(cardRegistry, predicateEvaluator)

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

        // Gate.OnceEachTurn: the per-turn *effect* budget behind "Do this only once each turn".
        // A synchronous test like WhenCondition, but it also *spends* the budget in the same step —
        // check and stamp are atomic, so two instances of the ability resolving back to back can
        // never both pass. Being inside any enclosing consent gate (the engine's lowering puts it
        // there) is what makes declining a "you may" free.
        if (gate is Gate.OnceEachTurn) {
            return executeOnceEachTurn(state, gate, effect, context)
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

        // Gate.MayPayAnyAmountOfLife: the life twin — prompt 0..payable life (MayPayLifeXContinuation).
        if (gate is Gate.MayPayAnyAmountOfLife) {
            return executeMayPayAnyAmountOfLife(state, effect, context)
        }

        // Gate.MayDecide: two cases where the former Effects.May skipped the prompt entirely.
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
                then.choices.none { checkFeasibility(state, context.controllerId, it.feasibilityCheck, predicateEvaluator = predicateEvaluator, manaSolver = manaSolver) }
            ) {
                return EffectResult.success(state)
            }
            // CR 701.59b — "if a player is given the choice to collect evidence but is unable to
            // exile cards with total mana value N or greater … they can't choose to collect
            // evidence." The option must be absent, not offered and refused, so an unreachable
            // threshold skips the prompt outright (the same treatment ReflexiveTriggerEffect gives
            // the "you may collect evidence N. When you do, …" shape).
            if (then is com.wingedsheep.sdk.scripting.effects.CollectEvidenceEffect) {
                val collector = TargetResolutionUtils
                    .resolvePlayerRef(then.player, context, state)
                if (collector == null ||
                    !com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver
                        .canCollect(state, collector, then.amount, predicateEvaluator = predicateEvaluator)
                ) {
                    return EffectResult.success(state)
                }
            }
            // "You may search your library … then shuffle" while the player can't search libraries
            // (Shadow of Doubt): they can't choose to search, so nothing happens — not even the
            // shuffle (the card's first ruling). Only a may whose action *leads* with the search is
            // skipped; a search nested behind another optional action ("you may sacrifice …. If you
            // do, search …") keeps its prompt, and the blocked search inside simply finds nothing.
            if (leadsWithSearch(effect.then) &&
                state.getEntity(context.controllerId)
                    ?.has<com.wingedsheep.engine.state.components.player.CantSearchLibrariesComponent>() == true
            ) {
                return effect.otherwise
                    ?.let { effectExecutor(state, it, context) }
                    ?: EffectResult.success(state)
            }
            // A declared feasibility that isn't met means the may-action is impossible — the player
            // "doesn't", so skip the prompt and run `otherwise` directly. This is the no-target
            // analogue of a targeted "may" with no legal targets falling to its else branch (e.g.
            // "you may sacrifice an artifact. If you don't, …" with no artifact taps you out).
            gate.feasibility?.let { check ->
                if (!checkFeasibility(state, context.controllerId, check, predicateEvaluator = predicateEvaluator, manaSolver = manaSolver)) {
                    return effect.otherwise
                        ?.let { effectExecutor(state, it, context) }
                        ?: EffectResult.success(state)
                }
            }
            // CR 608.2d — a player can't choose an option that's impossible. "You may exile eight
            // cards from your graveyard. If you do, this creature becomes prepared" can't be chosen
            // with fewer than eight cards there: the inner action gate could never clear its
            // CollectionNonEmpty bar. Offering the prompt anyway lets a "yes" partially perform the
            // exile — CR 609.3's do-as-much-as-possible, which governs *mandatory* instructions,
            // not an option that was never available — and spends those cards for nothing.
            if (!optionalActionCanClearItsBar(state, effect.then, context)) {
                return effect.otherwise
                    ?.let { effectExecutor(state, it, context) }
                    ?: EffectResult.success(state)
            }
        }

        val playerId = effect.decisionMaker
            ?.let { TargetResolutionUtils.resolvePlayerTarget(it, context, state) }
            ?: context.controllerId

        // Gate.MayPay over a waterbend-flagged PayManaCostEffect (Avatar: The Last Airbender): an
        // in-resolution "you may waterbend {N}. Otherwise, <otherwise>" gate. Instead of the plain
        // "pay?" yes/no + auto-tap composite, surface a mana-source decision that ALSO lists the
        // untapped artifacts/creatures the player may tap to help (each {1}), routing payment
        // through the shared waterbend machinery. Declining (or being unable to pay) runs `otherwise`.
        (gate as? Gate.MayPay)?.cost?.let { it as? PayManaCostEffect }?.takeIf { it.waterbend }?.let { pay ->
            return executeWaterbendPayment(state, playerId, pay.cost, effect.then, effect.otherwise, context)
        }

        // Persistent auto-answer yield (backlog §C): if the decision-maker has remembered a yes/no
        // for this ability, resolve the "you may" question without prompting and run the matching
        // branch. Scoped to Gate.MayDecide — the pure may-question — never a cost/pay gate, so a
        // yield can never spend mana or make a resource decision on the player's behalf (§C.6).
        if (gate is Gate.MayDecide) {
            val identity = context.abilityIdentity
            val auto = identity?.let { state.autoAnswerFor(playerId, it) }
            if (auto != null) {
                val sourceName = context.sourceId
                    ?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "ability"
                val note = AbilityAutoAnsweredEvent(context.sourceId ?: playerId, sourceName, playerId, auto)
                val branch = if (auto) effect.then else effect.otherwise
                val result = branch?.let { effectExecutor(state, it, context) } ?: EffectResult.success(state)
                return result.copy(events = listOf(note) + result.events)
            }
        }

        // Gate.MayPay: don't offer an impossible "yes" — fall straight through to `otherwise`.
        if (gate is Gate.MayPay && !canAfford(state, playerId, gate.cost, context)) {
            return effect.otherwise
                ?.let { effectExecutor(state, it, context) }
                ?: EffectResult.success(state)
        }

        // An optional *mana* payment (the lowered Effects.MayPay shape) keeps its bespoke UX —
        // a "Pay {cost}?" yes/no that, on "yes", routes through the mana-source-selection
        // continuations rather than the generic auto-tapping cost composite. See [OptionalManaPayment].
        effect.asOptionalManaPayment()?.let { mana ->
            return executeOptionalManaPayment(state, playerId, mana.cost, mana.then, context)
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val hint = when (gate) {
            // A dynamic hint wins over the static one: it is the only thing distinguishing two
            // instances of the same ability whose prompts are otherwise identical sentences.
            is Gate.MayDecide -> gate.dynamicHint?.let { renderDynamicHint(state, it, context) }
                ?: gate.hint ?: effect.hint
            is Gate.MayPay -> effect.hint
            is Gate.WhenCondition -> effect.hint // unreachable: handled by the synchronous branch above
            is Gate.DoAction -> effect.hint // unreachable: handled by the action-drain branch above
            is Gate.MayPayX -> effect.hint // unreachable: handled by the number-chooser branch above
            is Gate.MayPayAnyAmountOfLife -> effect.hint // unreachable: handled by the number-chooser branch above
            is Gate.OnceEachTurn -> effect.hint // unreachable: handled by the budget branch above
        }

        // For a pay-gate, label the "yes" button with the concrete cost — a dynamic cost
        // ("pay {4} for each chosen creature") renders its *computed* total ("Pay {8}") so the
        // player confirms a number, not a formula.
        val payLabel = (gate as? Gate.MayPay)?.let { computedCostLabel(state, it.cost, context) }

        val decision = { decisionId: String -> YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = effect.description,
            context = decisionContext(
                context,
                sourceName,
                inlineOnTrigger = (gate as? Gate.MayDecide)?.inlineOnTrigger ?: false
            ),
            yesText = payLabel ?: "Yes",
            noText = if (payLabel != null) "Don't pay" else "No",
            hint = hint
        ) }

        val continuation = GatedEffectContinuation(
            gate = gate,
            then = effect.then,
            otherwise = effect.otherwise,
            effectContext = context
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation))
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

        val decision = { decisionId: String -> YesNoDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay $manaCost?",
            context = decisionContext(context, sourceName),
            yesText = "Pay $manaCost",
            noText = "Don't pay"
        ) }

        val continuation = MayPayManaContinuation(
            playerId = playerId,
            sourceName = sourceName,
            manaCost = manaCost,
            effect = then,
            effectContext = context
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }

    /**
     * Resolve an in-resolution **waterbend** payment gate (Avatar: The Last Airbender):
     * "you may waterbend [manaCost]. Otherwise, [otherwise]." Surfaces a [SelectManaSourcesDecision]
     * that also lists the untapped artifacts/creatures the player may tap to help pay the generic
     * (each {1}, via [CostEnumerationUtils.findTapForGenericPermanents]); the shared
     * [MayPayManaSelectionContinuation] resumer then taps them, pays the remainder with mana, and
     * runs [then] on payment or [otherwise] on decline. If the player can't possibly pay (no mana
     * and no tappable permanents), [otherwise] runs immediately with no pointless prompt — mirroring
     * the Ward—Waterbend "can't pay → counter" short-circuit.
     */
    private fun executeWaterbendPayment(
        state: GameState,
        playerId: EntityId,
        manaCost: ManaCost,
        then: Effect,
        otherwise: Effect?,
        context: EffectContext
    ): EffectResult {
        val costUtils = CostEnumerationUtils(
            manaSolver, CostCalculator(cardRegistry, predicateEvaluator), predicateEvaluator, cardRegistry
        )
        val waterbendPermanents = costUtils.findTapForGenericPermanents(state, playerId, TapForGeneric.WATERBEND)
        val affordable = manaSolver.canPay(state, playerId, manaCost) ||
            costUtils.canAffordWithTapForGeneric(state, playerId, manaCost, waterbendPermanents)
        if (!affordable) {
            // Can't pay → the "unless" fires (e.g. discard a card).
            return otherwise?.let { effectExecutor(state, it, context) } ?: EffectResult.success(state)
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val sources = manaSolver.findAvailableManaSources(state, playerId)
        val sourceOptions = sources.map { source ->
            ManaSourceOption(
                entityId = source.entityId,
                name = source.name,
                producesColors = source.producesColors,
                producesColorless = source.producesColorless,
                requiresSacrifice = source.requiresSacrifice,
                manaAmount = source.manaAmount,
                requiresTappingAnotherPermanent = source.tapPermanentsSubCost != null
            )
        }
        val solution = manaSolver.solve(state, playerId, manaCost)
        val autoPaySuggestion = solution?.sources?.map { it.entityId } ?: emptyList()
        val waterbendOptions = waterbendPermanents.map {
            WaterbendPermanentChoice(it.entityId, it.name, it.isCreature)
        }

        val declineText = otherwise?.description?.replaceFirstChar { it.lowercase() }

        val decision = { decisionId: String -> SelectManaSourcesDecision(
            id = decisionId,
            playerId = playerId,
            prompt = if (declineText != null) {
                "Waterbend $manaCost (tap artifacts/creatures to help), or $declineText"
            } else {
                "Waterbend $manaCost (tap artifacts/creatures to help)"
            },
            context = decisionContext(context, sourceName),
            availableSources = sourceOptions,
            requiredCost = manaCost.toString(),
            autoPaySuggestion = autoPaySuggestion,
            canDecline = true,
            waterbendPermanents = waterbendOptions
        ) }

        val continuation = MayPayManaSelectionContinuation(
            playerId = playerId,
            sourceName = sourceName,
            manaCost = manaCost,
            effect = then,
            effectContext = context,
            availableSources = sourceOptions,
            autoPaySuggestion = autoPaySuggestion,
            waterbend = true,
            otherwise = otherwise
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }

    /**
     * Resolve a [Gate.OnceEachTurn] gate — the per-turn action budget behind the printed rider
     * "Do this only once each turn" (see [com.wingedsheep.sdk.scripting.TriggeredAbility.effectOncePerTurn]).
     *
     * The budget lives on the source permanent as a
     * [TriggeredAbilityEffectAppliedThisTurnComponent] keyed by ability id, so two capped abilities
     * on one permanent — or the same ability on two permanents — never share one. Spending it is
     * part of the same step as the check: the stamped state is what [GatedEffect.then] executes
     * against, so a second instance resolving immediately afterwards already sees a spent budget.
     * An already-spent gate falls to [GatedEffect.otherwise] (normally absent), which is how CR
     * 603.2h's "instances already on the stack do nothing as they resolve" is realised.
     *
     * A [Gate.OnceEachTurn.spend]`= false` gate is the read-only half of the same test: it lets
     * `TriggerProcessor` put a check *outside* an optional ability's consent gate so a spent
     * instance resolves silently instead of raising a pointless yes/no, without that check itself
     * consuming the turn's use.
     *
     * A source with no entity in state (it left the battlefield before this resolved) has nowhere
     * to keep the stamp; the effect still applies, matching how the other per-turn trackers behave
     * for a departed permanent.
     */
    private fun executeOnceEachTurn(
        state: GameState,
        gate: Gate.OnceEachTurn,
        effect: GatedEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId
        val alreadyApplied = sourceId
            ?.let { state.getEntity(it) }
            ?.get<TriggeredAbilityEffectAppliedThisTurnComponent>()
            ?.hasApplied(gate.abilityId) == true

        if (alreadyApplied) {
            return effect.otherwise
                ?.let { effectExecutor(state, it, context) }
                ?: EffectResult.success(state)
        }

        if (!gate.spend) {
            return effectExecutor(state, effect.then, context)
        }

        val stamped = if (sourceId != null && state.getEntity(sourceId) != null) {
            state.updateEntity(sourceId) { container ->
                val tracker = container.get<TriggeredAbilityEffectAppliedThisTurnComponent>()
                    ?: TriggeredAbilityEffectAppliedThisTurnComponent()
                container.with(tracker.withApplied(gate.abilityId))
            }
        } else {
            state
        }
        return effectExecutor(stamped, effect.then, context)
    }

    /**
     * Resolve a [Gate.MayPayX] gate (the lowered `Effects.MayPayX`). Computes the most generic mana
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

        val decision = { decisionId: String -> ChooseNumberDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay {X}? Choose X (0 to decline)",
            context = decisionContext(context, sourceName),
            minValue = 0,
            maxValue = maxAffordable
        ) }

        val continuation = MayPayXContinuation(
            playerId = playerId,
            sourceName = sourceName,
            effect = effect.then,
            maxX = maxAffordable,
            effectContext = context
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }

    /**
     * Resolve a [Gate.MayPayAnyAmountOfLife] gate. The most life the decision-maker can pay is their
     * life total, or nothing at all while they can't lose life (CR 119.8); with nothing payable the
     * gate falls through to [GatedEffect.otherwise] unprompted. Otherwise pauses with a 0..max
     * number chooser answered by [MayPayLifeXContinuation]; choosing 0 also runs `otherwise`.
     */
    private fun executeMayPayAnyAmountOfLife(
        state: GameState,
        effect: GatedEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = effect.decisionMaker
            ?.let { TargetResolutionUtils.resolvePlayerTarget(it, context, state) }
            ?: context.controllerId

        val maxPayable = if (state.isLifeLossLocked(playerId)) 0 else state.lifeTotal(playerId)
        if (maxPayable <= 0) {
            return effect.otherwise
                ?.let { effectExecutor(state, it, context) }
                ?: EffectResult.success(state)
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decision = { decisionId: String -> ChooseNumberDecision(
            id = decisionId,
            playerId = playerId,
            prompt = "Pay any amount of life? Choose an amount (0 to decline)",
            context = decisionContext(context, sourceName),
            minValue = 0,
            maxValue = maxPayable
        ) }

        val continuation = MayPayLifeXContinuation(
            playerId = playerId,
            sourceName = sourceName,
            effect = effect.then,
            otherwise = effect.otherwise,
            maxX = maxPayable,
            effectContext = context
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }

    /**
     * The [DecisionContext] every gate prompt in this executor carries.
     *
     * Beyond the source/trigger plumbing, it stamps the *subject* of the prompt from the enclosing
     * per-entity iteration ([EffectContext.iterationEntityId], the object
     * `EffectTarget.IterationEntity` names inside a `ForEachInGroup` body). A gate that runs
     * once per creature — Killing Wave's "sacrifice it unless you pay X life" — otherwise raises N
     * character-identical prompts, and the player has no way to tell which creature each covers.
     */
    private fun decisionContext(
        context: EffectContext,
        sourceName: String?,
        inlineOnTrigger: Boolean = false
    ): DecisionContext = DecisionContext(
        sourceId = context.sourceId,
        sourceName = sourceName,
        phase = DecisionPhase.RESOLUTION,
        triggeringEntityId = context.triggeringEntityId,
        inlineOnTrigger = inlineOnTrigger,
        subjectEntityId = context.iterationEntityId
    )

    /**
     * Fill a [DynamicHint]'s `{n}` from the resolving context, so a "that much damage" prompt says
     * which number *this* instance carries (CR 603.3d locks targets at trigger time, but the
     * amount is only read here, as the instance resolves).
     */
    private fun renderDynamicHint(state: GameState, hint: DynamicHint, context: EffectContext): String {
        val amount = dynamicAmountEvaluator.evaluate(state, hint.amount, context)
        return hint.template.replace(DynamicHint.PLACEHOLDER, amount.toString())
    }

    /**
     * Render a [Gate.MayPay] cost as a concrete "Pay …" button label, or null for shapes with no
     * single obvious rendering (life, sacrifice, composites) — those keep the plain "Yes". A
     * [PayDynamicManaCostEffect] shows its resolution-computed total, not the formula.
     */
    private fun computedCostLabel(state: GameState, cost: Effect, context: EffectContext): String? =
        when (cost) {
            is PayManaCostEffect -> "Pay ${cost.cost}"
            is PayDynamicManaCostEffect -> {
                // Match the `amount <= 0` short-circuit that `execute()` and `canAfford()` apply
                // before building a ManaCost: a non-positive amount pays nothing, and guarding here
                // also avoids `"{G}".repeat(negative)` throwing from inside label rendering.
                val amount = dynamicAmountEvaluator.evaluate(state, cost.amount, context).coerceAtLeast(0)
                "Pay ${PayDynamicManaCostExecutor.dynamicManaCost(amount, cost.color)}"
            }
            is PayExactCountersEffect -> {
                val amount = dynamicAmountEvaluator.evaluate(state, cost.amount, context).coerceAtLeast(0)
                "Pay $amount ${cost.counterType.printed} counters"
            }
            is RemoveCountersEffect -> {
                val amount = dynamicAmountEvaluator.evaluate(state, cost.count, context).coerceAtLeast(0)
                "Remove $amount ${cost.counterType.printed} counter${if (amount != 1) "s" else ""}"
            }
            is PayLifeEffect -> "Pay ${cost.amount} life"
            is PayDynamicLifeEffect -> {
                val amount = dynamicAmountEvaluator.evaluate(state, cost.amount, context).coerceAtLeast(0)
                "Pay $amount life"
            }
            else -> null
        }

    /**
     * Whether [playerId] can pay [cost] right now. Mirrors the former
     * `OptionalCostEffectExecutor`: recognizes the payment primitives that appear in a
     * [Gate.MayPay] cost slot ([PayManaCostEffect], [PayDynamicManaCostEffect], [PayLifeEffect],
     * [PayManaCostRepeatedlyEffect], and a [CompositeEffect] composing them). The dynamic-mana branch
     * charges the cost's own `payer`, so
     * affordability stays correct even when it differs from the gate's decisionMaker. Unknown shapes
     * fail open (assumed payable) so exotic cost pipelines still prompt and abort later via the
     * resumer's `stopOnError` composite.
     */
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
                amount <= 0 || manaSolver.canPay(
                    state, payerId, PayDynamicManaCostExecutor.dynamicManaCost(amount, cost.color)
                )
            }
            is PayExactCountersEffect -> {
                val payer = TargetResolutionUtils.resolvePlayerRef(cost.player, context, state)
                val amount = dynamicAmountEvaluator.evaluate(state, cost.amount, context).coerceAtLeast(0)
                payer != null && com.wingedsheep.engine.mechanics.cost.PlayerCounterPayment.available(
                    state, payer, cost.counterType
                ) >= amount
            }
            is PayLifeEffect -> {
                state.canPayLife(playerId, cost.amount) // CR 810.9a / 119.8
            }
            is PayDynamicLifeEffect -> {
                // Resolve the cost's own payer; a computed amount of <= 0 is free (CR 119.4).
                val amount = dynamicAmountEvaluator.evaluate(state, cost.amount, context)
                val payerId = TargetResolutionUtils
                    .resolvePlayerTarget(EffectTarget.PlayerRef(cost.payer), context, state)
                    ?: playerId
                state.canPayLife(payerId, amount)
            }
            // "You may pay {1} up to three times" as a gate cost: the repeated payment's floor is
            // one repetition, so a payer who can't afford even that must not be offered the "yes"
            // (the cost would then error and `stopOnError` would swallow the whole payoff). Mirrors
            // ReflexiveTriggerEffectExecutor.isActionFeasible, which scores the same shape.
            is PayManaCostRepeatedlyEffect -> PayManaCostRepeatedlyExecutor.affordableRepetitions(
                state, playerId, cost.cost, cost.maxTimes, cardRegistry,
                predicateEvaluator = predicateEvaluator
            ) >= 1
            // Resolution-time collect evidence is also a payable action (Izoni): the player may
            // choose it only when their graveyard can meet the full mana-value threshold.
            is CollectEvidenceEffect -> {
                val collector = TargetResolutionUtils.resolvePlayerRef(cost.player, context, state)
                collector != null && CollectEvidenceResolver.canCollect(state, collector, cost.amount, predicateEvaluator = predicateEvaluator)
            }
            // "You may remove that many reprieve counters from this creature. If you do, …"
            // (Magnanimous Magistrate) — a cost is paid in full or not at all, so the "yes" exists
            // only while the permanent carries the whole amount. The executor alone would clamp to
            // what is there and report a partial removal as success.
            is RemoveCountersEffect -> {
                val amount = dynamicAmountEvaluator.evaluate(state, cost.count, context)
                val holder = context.resolveTarget(cost.target, state)
                amount <= 0 || (holder != null &&
                    (state.getEntity(holder)?.get<CountersComponent>()?.getCount(cost.counterType) ?: 0) >= amount)
            }
            is CompositeEffect -> cost.effects.all { canAfford(state, playerId, it, context) }
            // "You may sacrifice [filter]" — payable only if the player controls enough matching
            // permanents (`any = true`, "sacrifice any number", is always payable: zero is legal).
            // Without this the gate fails open and offers an impossible "yes": Pippin's Bravery with
            // no Food still lets you choose "Sacrifice a Food" and wrongly take the +4/+4 branch.
            is com.wingedsheep.sdk.scripting.effects.SacrificeEffect -> cost.any || run {
                val fodder = BattlefieldFilterUtils.findMatchingOnBattlefield(
                    state, cost.filter.youControl(), PredicateContext(controllerId = playerId),
                    predicateEvaluator = predicateEvaluator
                ).filterNot { cost.excludeSource && it == context.sourceId }
                fodder.size >= cost.count
            }
            else -> true
        }

    /**
     * Can the "if you do" action under a "you may" still clear its own success bar?
     *
     * Recognizes one idiom — "you may [choose exactly N cards from a gathered pool and move
     * them]. If you do, …": a [Gate.DoAction] scored by [SuccessCriterion.CollectionNonEmpty]
     * whose named collection is filled by a [SelectFromCollectionEffect] over a
     * [GatherCardsEffect]'s pool. When that pool holds fewer cards than the criterion demands, no
     * answer to the yes/no can make the gate succeed, so the option isn't a legal choice at all
     * (CR 608.2d) and the prompt is skipped. Anything else answers `true` — prompt as before.
     *
     * The count is deliberately generous: of a selection's `restrictions`, only
     * [SelectionRestriction.OnePerCardName] is applied (the pool then offers one card per distinct
     * name — "reveal exactly two cards with different names", Extrapolate the Impossible); the
     * others can narrow it further but aren't applied here, so the prompt is only ever withheld when
     * the payment is impossible outright. Gathering is a pure read — it stores a collection and changes no state and emits no
     * events — so probing it here costs nothing and can't be observed.
     */
    private fun optionalActionCanClearItsBar(
        state: GameState,
        then: Effect,
        context: EffectContext
    ): Boolean {
        var inner = then
        while (true) {
            val gated = inner as? GatedEffect ?: return true
            when (val gate = gated.gate) {
                // The `effectOncePerTurn` lowering sits between the consent gate and the action.
                is Gate.OnceEachTurn -> inner = gated.then
                is Gate.DoAction -> return actionCanClearItsBar(state, gate, context)
                else -> return true
            }
        }
    }

    private fun actionCanClearItsBar(
        state: GameState,
        gate: Gate.DoAction,
        context: EffectContext
    ): Boolean {
        val criterion = gate.successCriterion as? SuccessCriterion.CollectionNonEmpty ?: return true
        val steps = (gate.action as? CompositeEffect)?.effects ?: listOf(gate.action)
        val select = steps.filterIsInstance<SelectFromCollectionEffect>()
            .firstOrNull { it.storeSelected == criterion.name } ?: return true
        if (select.selection !is SelectionMode.ChooseExactly) return true
        val gather = steps.filterIsInstance<GatherCardsEffect>()
            .firstOrNull { it.storeAs == select.from } ?: return true
        val pool = effectExecutor(state, gather, context).updatedCollections[select.from] ?: return true
        val eligible = if (select.filter == GameObjectFilter.Any) {
            pool
        } else {
            val predicateContext = PredicateContext.fromEffectContext(context)
            pool.filter { cardId ->
                predicateEvaluator.matches(state, state.projectedState, cardId, select.filter, predicateContext)
            }
        }
        val achievable = if (select.restrictions.any { it is SelectionRestriction.OnePerCardName }) {
            eligible.mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }.toSet().size
        } else {
            eligible.size
        }
        return achievable >= criterion.min
    }

    /**
     * Resolve a [Gate.DoAction] gate (the lowered `Effects.IfYouDo`). Follows the former
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
            then = effect.then,
            otherwise = effect.otherwise,
            successCriterion = gate.successCriterion,
            snapshot = snapshot,
            effectContext = context
        )
        val stateWithCont = state.pushContinuation(continuation)

        val result = effectExecutor(stateWithCont, gate.action, context)

        if (result.outcome is Outcome.Paused) {
            // Action paused; leave the continuation on the stack for the auto-resumer.
            return result
        }

        // Action ran synchronously (success or recoverable error). Pop our pre-pushed
        // continuation and evaluate the outcome inline. Collections the action produced
        // are merged into the context so a CollectionNonEmpty criterion (and the chosen
        // branch) can read them — the paused path gets the same data via
        // exposeCollectionsToNextFrame updating the pre-pushed frame.
        val (_, stateWithoutCont) = result.state.popContinuation()
        val contextWithCollections = if (result.updatedCollections.isEmpty()) context else context.copy(
            pipeline = context.pipeline.copy(
                storedCollections = context.pipeline.storedCollections + result.updatedCollections
            )
        )
        return evaluateAndDispatch(
            state = stateWithoutCont,
            then = effect.then,
            otherwise = effect.otherwise,
            criterion = gate.successCriterion,
            snapshot = snapshot,
            effectContext = contextWithCollections,
            priorEvents = result.events,
            effectExecutor = effectExecutor
        )
    }

    /**
     * Capture the data the [SuccessCriterion] needs to evaluate the post-action delta.
     *
     * For [SuccessCriterion.Auto], the shape probe is [SuccessCriterion.Auto.canInfer]'s
     * walkers — the SDK owns them so card-load validation and this snapshot recognize
     * exactly the same shapes — and the destination zone's pre-execution size is recorded:
     * - a terminal pipeline [MoveCollectionEffect] (multi-object moves), and
     * - a terminal single-target [MoveToZoneEffect] whose target is [EffectTarget.Self]
     *   (e.g. "exile this card from your graveyard. If you do, …" — Council's Deliberation).
     *
     * The collection move is checked first so a pipeline that ends in one keeps its existing
     * semantics. Shapes the probe rejects are a card-load validation error, so an empty
     * snapshot here only happens for runtime-unresolvable pieces (a destination player that
     * doesn't resolve), where [evaluateAuto] treats the action as performed.
     */
    private fun captureSnapshot(
        state: GameState,
        action: Effect,
        criterion: SuccessCriterion,
        context: EffectContext
    ): GatedActionSnapshot {
        if (criterion !is SuccessCriterion.Auto) return GatedActionSnapshot()

        SuccessCriterion.Auto.terminalCollectionMove(action)?.let { move ->
            val destination = move.destination as? CardDestination.ToZone ?: return GatedActionSnapshot()
            val ownerId = resolvePlayer(destination.player, context, state) ?: return GatedActionSnapshot()
            return zoneSnapshot(state, ownerId, destination.zone)
        }

        SuccessCriterion.Auto.terminalSingleMove(action)?.let { move ->
            // Only a move of the source or of a loop's current object names a concrete moved
            // entity here; the destination zone is owned by that entity's owner (e.g. self-exile
            // from a graveyard lands in that card's owner's exile).
            val movedId = when (move.target) {
                EffectTarget.Self -> context.sourceId
                EffectTarget.IterationEntity -> context.iterationEntityId
                else -> null
            } ?: return GatedActionSnapshot()
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

    private fun resolvePlayer(player: Player, context: EffectContext, state: GameState): EntityId? =
        TargetResolutionUtils.resolvePlayerRef(player, context, state) ?: context.controllerId

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
            effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
            // Events to *read* when evaluating an event-based criterion (SuccessCriterion.DamageDealt).
            // Defaults to [priorEvents]; the auto-resumer passes the accumulated frame events here
            // (already emitted by the paused action) while keeping [priorEvents] empty so they're
            // not prepended twice when the outer merge re-adds them.
            evaluationEvents: List<GameEvent> = priorEvents
        ): EffectResult {
            val happened = evaluate(state, criterion, snapshot, effectContext, evaluationEvents)
            val branch = if (happened) then else otherwise
                ?: return EffectResult.success(state, priorEvents)
            // The branch is a later part of the same effect as the action, so it may find the object
            // the action just moved to a public zone (CR 400.7j) — "exile it. If you do, create a
            // token that's a copy of that creature" (Kinzu of the Bleak Coven). Same authorization
            // CompositeEffectExecutor applies between its steps.
            val branchResult = effectExecutor(state, branch, effectContext.authorizeObjectMoves(evaluationEvents))
            return branchResult.copy(events = priorEvents + branchResult.events)
        }

        /**
         * Did the action accomplish its work, given the snapshot taken before it ran?
         *
         * [effectContext] carries the action's pipeline collections: the synchronous path
         * merges the action result's `updatedCollections` in before dispatch, and the paused
         * path receives them via `exposeCollectionsToNextFrame` updating the pre-pushed
         * [GatedActionContinuation] when the action's last collection-producing step resolves.
         */
        private fun evaluate(
            state: GameState,
            criterion: SuccessCriterion,
            snapshot: GatedActionSnapshot,
            effectContext: EffectContext,
            priorEvents: List<GameEvent>
        ): Boolean = when (criterion) {
            is SuccessCriterion.Always -> true
            is SuccessCriterion.Auto -> evaluateAuto(state, snapshot)
            is SuccessCriterion.CollectionNonEmpty ->
                (effectContext.pipeline.storedCollections[criterion.name]?.size ?: 0) >= criterion.min
            is SuccessCriterion.DamageDealt -> evaluateDamageDealt(criterion, effectContext, priorEvents)
            is SuccessCriterion.ControlChanged -> evaluateControlChanged(priorEvents)
            is SuccessCriterion.CountersAdded -> priorEvents.any {
                it is com.wingedsheep.engine.core.CountersAddedEvent && it.amount > 0
            }
            is SuccessCriterion.CountersRemoved -> evaluateCountersRemoved(priorEvents)
            is SuccessCriterion.PermanentsSacrificed -> evaluatePermanentsSacrificed(priorEvents)
            is SuccessCriterion.TurnedFaceUp -> evaluateTurnedFaceUp(priorEvents)
        }

        /**
         * Did the gated action actually turn a permanent face up? Scans the action's own events for
         * a [TurnFaceUpEvent]. `TurnFaceUpExecutor` emits one only when the permanent really flipped:
         * a manifested/cloaked instant or sorcery card is revealed and left face down (CR 701.40g /
         * 701.58g, a [CardsRevealedEvent] instead), and an already-face-up permanent produces no
         * event at all. Both are the "you can't" case Etrata, Deadly Fugitive's fallback branch needs.
         */
        private fun evaluateTurnedFaceUp(priorEvents: List<GameEvent>): Boolean =
            priorEvents.any { event -> event is TurnFaceUpEvent }

        /**
         * Did the gated action actually sacrifice something? Scans the action's own events for a
         * [PermanentsSacrificedEvent] with a non-empty permanent list. A player who controls nothing
         * the filter matches sacrifices nothing and emits no such event — exactly the "you didn't do
         * it" case the gate must catch (Garruk, the Veil-Cursed's −1 on an empty board).
         */
        private fun evaluatePermanentsSacrificed(priorEvents: List<GameEvent>): Boolean =
            priorEvents.any { event ->
                event is com.wingedsheep.engine.core.PermanentsSacrificedEvent &&
                    event.permanentIds.isNotEmpty()
            }

        /**
         * Did the gated action actually change control of a permanent? Scans the action's events for
         * a [ControlChangedEvent] whose old and new controllers differ. The events scanned are only
         * the gated action's own (the executor passes the action result's events), so a bare presence
         * check is correctly scoped to this ability; the old/new comparison rejects the no-op case
         * where the control-change effect ran against a permanent already controlled by the intended
         * new controller, and a control change that never happened (illegal/missing permanent target
         * at resolution) emits no such event at all.
         */
        /**
         * Did the gated action actually take a counter off something? Scans the action's own
         * events for a positive-amount [CountersRemovedEvent] — a removal against a permanent with
         * no such counter (or one that has left the battlefield) emits none, which is exactly the
         * "you didn't do it" case the gate must catch.
         */
        private fun evaluateCountersRemoved(priorEvents: List<GameEvent>): Boolean =
            priorEvents.any { event -> event is CountersRemovedEvent && event.amount > 0 }

        private fun evaluateControlChanged(priorEvents: List<GameEvent>): Boolean =
            priorEvents.any { event ->
                event is ControlChangedEvent && event.oldControllerId != event.newControllerId
            }

        /**
         * Did the gated action actually deal damage from this ability's source? Scans the action's
         * events for a [DamageDealtEvent] with positive amount sourced from `effectContext.sourceId`
         * (CR 119.x — damage that was prevented/replaced emits no such event). When the criterion's
         * recipient is [DamageRecipient.Controller], the damaged target must be the controller.
         */
        private fun evaluateDamageDealt(
            criterion: SuccessCriterion.DamageDealt,
            effectContext: EffectContext,
            priorEvents: List<GameEvent>
        ): Boolean {
            val sourceId = effectContext.sourceId
            val controllerId = effectContext.controllerId
            return priorEvents.any { event ->
                event is DamageDealtEvent &&
                    event.amount > 0 &&
                    (sourceId == null || event.sourceId == sourceId) &&
                    when (criterion.recipient) {
                        DamageRecipient.Any -> true
                        DamageRecipient.Controller -> event.targetId == controllerId
                    }
            }
        }

        private fun evaluateAuto(state: GameState, snapshot: GatedActionSnapshot): Boolean {
            val owner = snapshot.destinationZoneOwner
            val zone = snapshot.destinationZoneType
            if (owner == null || zone == null) {
                // No zone-move probe was capturable. Card-load validation rejects Auto on action
                // shapes the probe doesn't recognize, so this is reached only when a recognized
                // shape couldn't be resolved at runtime (destination player didn't resolve, source
                // already gone) — treat the action as performed rather than failing mid-resolution.
                return true
            }
            val postSize = state.zones[ZoneKey(owner, zone)]?.size ?: 0
            return postSize > snapshot.destinationZonePreSize
        }
    }

    /** True when [effect]'s first step is a library search (a `GatherCardsEffect(search = true)`). */
    private fun leadsWithSearch(effect: Effect): Boolean = when (effect) {
        is com.wingedsheep.sdk.scripting.effects.GatherCardsEffect -> effect.search
        is com.wingedsheep.sdk.scripting.effects.CompositeEffect ->
            effect.effects.firstOrNull()?.let { leadsWithSearch(it) } == true
        else -> false
    }
}
