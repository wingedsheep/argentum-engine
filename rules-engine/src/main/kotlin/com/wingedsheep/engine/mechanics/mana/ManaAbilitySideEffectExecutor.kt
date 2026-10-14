package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.tapForMana
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
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
    private val zones: ZoneTransitionService,
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
            val production = solution.manaProduced[source.entityId]
            val (tappedState, tapEvents) = tapForMana(
                currentState, source.entityId, controllerId, production?.producedColors.orEmpty()
            )
            currentState = tappedState
            events.addAll(tapEvents)

            // Resolved once and shared: the activation event, the life cost and the side effects
            // all want the same ability, and this loop runs for every auto-tapped source of every payment.
            val ability = ManaAbilityLifeCost.activatedManaAbility(cardRegistry, currentState, source.entityId, production, controllerId, zones.predicateEvaluator)

            // Auto-tapping a source *is* the player activating its mana ability — the fast path is
            // a UI shortcut, not a different game action (CR 605.3). Emit the activation event the
            // manual path emits so "whenever you activate an ability" triggers see it (Elrond,
            // Moon-Reader off an auto-tapped Llanowar Elves). Emitted after the TappedEvent: the
            // tap is the cost, and the ability is activated once its costs are paid.
            activationEvent(currentState, source.entityId, controllerId, ability)?.let(events::add)

            // The rest of the cost: life printed on the ability (Mana Confluence) plus any Thran
            // Portal tax — the fast path only paid the tap.
            val (paid, lifeEvents) = ManaAbilityLifeCost.pay(zones, currentState, source.entityId, controllerId, ability)
            currentState = paid
            events.addAll(lifeEvents)

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
     * Tap [sourceId] for mana and charge the life its mana ability costs ([ManaAbilityLifeCost]) —
     * for the auto-pay paths that tap a chosen source directly. [production] is the mana the tap
     * is credited with, when known.
     */
    fun tapForManaPayingLife(
        state: GameState,
        sourceId: EntityId,
        tapperId: EntityId,
        production: ManaProduction? = null,
    ): Pair<GameState, List<GameEvent>> = ManaAbilityLifeCost.tapForManaPayingLife(zones, state, sourceId, tapperId, production)

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
        state, sourceId, controllerId, ManaAbilityLifeCost.activatedManaAbility(cardRegistry, state, sourceId, ManaProduction(color = producedColor), controllerId, zones.predicateEvaluator)
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

    fun runSideEffects(
        state: GameState,
        sourceId: EntityId,
        producedColor: Color?,
        controllerId: EntityId,
    ): Pair<GameState, List<GameEvent>> = runSideEffects(
        state, sourceId, controllerId, ManaAbilityLifeCost.activatedManaAbility(cardRegistry, state, sourceId, ManaProduction(color = producedColor), controllerId, zones.predicateEvaluator)
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

    private fun nonManaSubEffects(effect: Effect): List<Effect> = when (effect) {
        is CompositeEffect -> effect.effects.filterNot { ManaAbilityLifeCost.isManaEffect(it) }
        else -> emptyList()  // single-effect mana abilities have nothing extra to run
    }

    companion object {
        /**
         * Stand-in instance for default-constructed contexts (e.g. a [CombatManager]
         * built without an [EngineServices] wiring). Side effects are dropped on the
         * floor — production code must use the executor wired by [EngineServices].
         */
        fun noOp(zones: ZoneTransitionService): ManaAbilitySideEffectExecutor =
            ManaAbilitySideEffectExecutor(zones, zones.cardRegistry) { state, _, _ ->
                EffectResult.success(state)
            }
    }
}
