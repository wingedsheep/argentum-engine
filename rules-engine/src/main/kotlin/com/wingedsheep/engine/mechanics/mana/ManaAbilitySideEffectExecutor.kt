package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.life.LifePaymentService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Runs the non-mana side effects of an activated mana ability when a source is
 * auto-tapped to pay a cost.
 *
 * Auto-tap fast paths (e.g. spell casting, cycling, combat tax) bypass the normal
 * activated-ability flow: they tap the source and credit its produced mana directly
 * to the payment, skipping the [com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler].
 * For most lands that's correct — the ability is just "{T}: Add {X}" — but pain
 * lands like Adarkar Wastes carry damage as part of the ability's effect
 * (`{T}: Add {W} or {U}. This land deals 1 damage to you.`). Without this helper
 * that damage is silently lost.
 *
 * The helper finds the activated mana ability that matches the produced color and
 * executes everything in its effect chain *except* the mana-producing pieces
 * (which the auto-tap path has already accounted for).
 */
class ManaAbilitySideEffectExecutor(
    private val cardRegistry: CardRegistry,
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) {

    /**
     * Run side effects for a single auto-tapped source.
     *
     * @param state Current game state (already mutated by the caller to reflect tap).
     * @param sourceId Permanent that was tapped.
     * @param producedColor Color the source produced for the payment, or null for colorless.
     * @param controllerId Player who controls the source / paid the cost.
     */
    /**
     * Tap every source in [solution] (emitting [TappedEvent]) and run any
     * non-mana side effects of the matching mana ability. This is the
     * one-shot form for callers that already have a [ManaSolution] from
     * [ManaSolver]; the produced mana itself is still consumed separately
     * via [ManaSolution.manaProduced].
     */
    fun tapSourcesWithSideEffects(
        state: GameState,
        solution: ManaSolution,
        controllerId: EntityId
    ): Pair<GameState, List<GameEvent>> {
        var currentState = state
        val events = mutableListOf<GameEvent>()
        for (source in solution.sources) {
            val (tappedState, event) = tap(currentState, source.entityId)
            currentState = tappedState
            event?.let(events::add)

            val production = solution.manaProduced[source.entityId]
            // Resolved once and shared: both the activation event and the side effects want the
            // same ability, and this loop runs for every auto-tapped source of every payment.
            val ability = matchingManaAbility(currentState, source.entityId, production?.color)

            // Auto-tapping a source *is* the player activating its mana ability — the fast path is
            // a UI shortcut, not a different game action (CR 605.3). Emit the activation event the
            // manual path emits so "whenever you activate an ability" triggers see it (Elrond,
            // Moon-Reader off an auto-tapped Llanowar Elves). Emitted after the TappedEvent: the
            // tap is the cost, and the ability is activated once its costs are paid.
            activationEvent(currentState, source.entityId, controllerId, ability)?.let(events::add)

            val (after, sideEvents) = runSideEffects(
                state = currentState,
                sourceId = source.entityId,
                controllerId = controllerId,
                matchingAbility = ability,
            )
            currentState = after
            events.addAll(sideEvents)
        }
        return currentState to events
    }

    /**
     * The [AbilityActivatedEvent] for an auto-tapped mana source, or null if [sourceId] isn't a
     * card (nothing to name in the event).
     *
     * `costsTap` is true by construction — this path only ever reaches sources it taps — so the
     * Antiquities "without {T} in its activation cost" template correctly ignores these. `isExhaust`
     * is read off the matching printed ability where one is found; an intrinsic land mana ability
     * has no [ActivatedAbility] entry to consult and is never exhaust anyway.
     */
    fun activationEvent(
        state: GameState,
        sourceId: EntityId,
        producedColor: Color?,
        controllerId: EntityId,
    ): AbilityActivatedEvent? = activationEvent(
        state, sourceId, controllerId, matchingManaAbility(state, sourceId, producedColor)
    )

    private fun activationEvent(
        state: GameState,
        sourceId: EntityId,
        controllerId: EntityId,
        matchingAbility: ActivatedAbility?,
    ): AbilityActivatedEvent? {
        val card = state.getEntity(sourceId)?.get<CardComponent>() ?: return null
        return AbilityActivatedEvent(
            sourceId = sourceId,
            sourceName = card.name,
            controllerId = controllerId,
            abilityEntityId = null,
            costsTap = true,
            isManaAbility = true,
            isExhaust = matchingAbility?.isExhaust == true
        )
    }

    /** The printed mana ability of [sourceId] that produced [producedColor], if there is one. */
    private fun matchingManaAbility(
        state: GameState,
        sourceId: EntityId,
        producedColor: Color?,
    ): ActivatedAbility? {
        val card = state.getEntity(sourceId)?.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return null
        return cardDef.script.activatedAbilities
            .filter { it.isManaAbility }
            .firstOrNull { abilityProducesColor(it, producedColor) }
    }

    fun runSideEffects(
        state: GameState,
        sourceId: EntityId,
        producedColor: Color?,
        controllerId: EntityId,
    ): Pair<GameState, List<GameEvent>> = runSideEffects(
        state, sourceId, controllerId, matchingManaAbility(state, sourceId, producedColor)
    )

    private fun runSideEffects(
        state: GameState,
        sourceId: EntityId,
        controllerId: EntityId,
        matchingAbility: ActivatedAbility?,
    ): Pair<GameState, List<GameEvent>> {
        if (matchingAbility == null) return state to emptyList()

        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Pain modeled as part of the ability's *cost* (e.g. Starting Town's
        // "{T}, Pay 1 life: Add one mana of any color") — the auto-tap fast path only
        // pays the tap, so any life-payment cost atom would otherwise be silently skipped.
        // (Pain modeled as an *effect*, like Adarkar Wastes, is handled by the sub-effect
        // loop below.) The solver already tracks these via ManaSource.hasPainCost for tap
        // priority, but never deducts the life.
        val lifeCost = payLifeCost(matchingAbility.cost)
        if (lifeCost > 0) {
            LifePaymentService.pay(currentState, controllerId, lifeCost)?.let { (afterLife, lifeEvents) ->
                currentState = afterLife
                events.addAll(lifeEvents)
            }
        }

        val sideEffects = nonManaSubEffects(matchingAbility.effect)
        if (sideEffects.isEmpty()) return currentState to events

        val context = EffectContext(
            sourceId = sourceId,
            controllerId = controllerId,
        )

        for (sub in sideEffects) {
            val result = effectExecutor(currentState, sub, context)
            currentState = result.state
            events.addAll(result.events)
            // Side effects from auto-tap should never pause for player decisions
            // (mana abilities don't use the stack), so we treat any pause as a
            // no-op and continue. In practice every printed mana-ability side
            // effect is fully resolved with controller info alone.
        }
        return currentState to events
    }

    /**
     * Sum of life-payment ([CostAtom.PayLife]) amounts in a mana ability's cost, recursing
     * through composite costs (e.g. `{T}, Pay 1 life`). Returns 0 when the cost has no
     * life component.
     */
    private fun payLifeCost(cost: AbilityCost): Int = when (cost) {
        is AbilityCost.Atom -> (cost.atom as? CostAtom.PayLife)?.amount ?: 0
        is AbilityCost.Composite -> cost.costs.sumOf { payLifeCost(it) }
        else -> 0
    }

    private fun abilityProducesColor(ability: ActivatedAbility, color: Color?): Boolean =
        manaSubEffects(ability.effect).any { effect -> effectProduces(effect, color) }

    private fun effectProduces(effect: Effect, color: Color?): Boolean = when (effect) {
        is AddManaEffect -> effect.color == color
        is AddColorlessManaEffect -> color == null
        is AddManaOfChoiceEffect,
        is AddAnyColorManaSpendOnChosenTypeEffect -> color != null  // any non-null color
        is AddDynamicManaEffect -> color != null && color in effect.allowedColors
        else -> false
    }

    private fun manaSubEffects(effect: Effect): List<Effect> = when (effect) {
        is CompositeEffect -> effect.effects.filter { isManaEffect(it) }
        else -> if (isManaEffect(effect)) listOf(effect) else emptyList()
    }

    private fun nonManaSubEffects(effect: Effect): List<Effect> = when (effect) {
        is CompositeEffect -> effect.effects.filterNot { isManaEffect(it) }
        else -> emptyList()  // single-effect mana abilities have nothing extra to run
    }

    private fun isManaEffect(effect: Effect): Boolean = effect is AddManaEffect ||
        effect is AddColorlessManaEffect ||
        effect is AddManaOfChoiceEffect ||
        effect is AddAnyColorManaSpendOnChosenTypeEffect ||
        effect is AddDynamicManaEffect

    companion object {
        /**
         * Stand-in instance for default-constructed contexts (e.g. a [CombatManager]
         * built without an [EngineServices] wiring). Side effects are dropped on the
         * floor — production code must use the executor wired by [EngineServices].
         */
        fun noOp(cardRegistry: CardRegistry): ManaAbilitySideEffectExecutor =
            ManaAbilitySideEffectExecutor(cardRegistry) { state, _, _ ->
                EffectResult.success(state)
            }
    }
}
