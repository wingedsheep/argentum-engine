package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.core.AnyColorTapBonus
import com.wingedsheep.engine.core.ChooseAnyColorTapBonusContinuation
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.continuations.CheckForMore
import com.wingedsheep.engine.mechanics.mana.ManaColorSetResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaColorSet

/**
 * Resolves "additional one mana of any color" tap bonuses from [AdditionalManaOnTap] auras with
 * `anyColor = true` (Fertile Ground). These are triggered mana abilities that resolve off the
 * stack, but unlike fixed-color bonuses they require a per-tap color choice. The resolver drives
 * the pending bonuses one at a time, pausing for a [ChooseAnyColorTapBonusContinuation] when a
 * color choice is needed and resuming with the remaining bonuses afterward.
 *
 * Fixed-color and mirror-color tap bonuses are handled directly (no decision) by
 * `ActivateAbilityHandler.resolveAdditionalManaOnTap` / `resolveAdditionalManaOnSourceTap`; this
 * resolver only handles the any-color choice case. The shared [AddManaOfChoiceExecutor] keeps the
 * actual mana addition (and its events) consistent with normal "add one mana of any color".
 */
class TappedForManaBonusResolver(
    private val cardRegistry: CardRegistry,
    private val dynamicAmountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator(),
    private val decisionHandler: DecisionHandler = DecisionHandler(),
) {
    private val manaExecutor = AddManaOfChoiceExecutor(cardRegistry)

    /**
     * Collect the any-color tap bonuses owed when the land [landId] (controlled by [landController])
     * is tapped for mana — one per attached [AdditionalManaOnTap] aura with `anyColor = true`,
     * with its bonus amount already evaluated.
     */
    fun collect(state: GameState, landId: EntityId, landController: EntityId): List<AnyColorTapBonus> {
        val result = mutableListOf<AnyColorTapBonus>()
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            if (container.get<AttachedToComponent>()?.targetId != landId) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (staticAbility in cardDef.script.staticAbilities) {
                val bonus = staticAbility as? AdditionalManaOnTap ?: continue
                if (!bonus.anyColor) continue
                val amount = dynamicAmountEvaluator.evaluate(state, bonus.amount, contextFor(state, entityId, landController))
                if (amount <= 0) continue
                result.add(AnyColorTapBonus(auraId = entityId, controllerId = landController, amount = amount))
            }
        }
        return result
    }

    /**
     * Resolve [items] in order, carrying [accumulatedEvents] through. Returns success once all
     * bonuses are added, or a paused result (with a [ChooseAnyColorTapBonusContinuation] pushed)
     * when the head bonus needs a color choice.
     */
    fun drive(
        state: GameState,
        items: List<AnyColorTapBonus>,
        accumulatedEvents: List<GameEvent>,
    ): ExecutionResult {
        if (items.isEmpty()) return ExecutionResult.success(state, accumulatedEvents)

        val item = items.first()
        val remaining = items.drop(1)
        val available = ManaColorSetResolver.resolve(
            ManaColorSet.AnyColor, state, state.projectedState, item.auraId, item.controllerId, cardRegistry
        )
        if (available.isEmpty()) return drive(state, remaining, accumulatedEvents)

        // A single legal color needs no choice; add it directly. (Plain any-color always offers
        // five, so this only short-circuits if a future colorSet narrowing is introduced.)
        val singleColor = available.singleOrNull()
        if (singleColor != null) {
            val added = manaExecutor.addManaToPool(state, bonusEffect(item.amount), contextFor(state, item.auraId, item.controllerId), singleColor, available)
            return drive(added.state, remaining, accumulatedEvents + added.events)
        }

        val sourceName = state.getEntity(item.auraId)?.get<CardComponent>()?.name
        val decision = decisionHandler.createColorDecision(
            state = state,
            playerId = item.controllerId,
            sourceId = item.auraId,
            sourceName = sourceName,
            prompt = "Choose a color for the additional mana",
            phase = DecisionPhase.RESOLUTION,
            availableColors = available,
        )
        val continuation = ChooseAnyColorTapBonusContinuation(
            decisionId = decision.pendingDecision!!.id,
            current = item,
            remaining = remaining,
        )
        return ExecutionResult.paused(
            decision.state.pushContinuation(continuation),
            decision.pendingDecision,
            accumulatedEvents + decision.events,
        )
    }

    /** Resume after the controller picks the color for [continuation]'s current bonus. */
    fun resume(
        state: GameState,
        continuation: ChooseAnyColorTapBonusContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore,
    ): ExecutionResult {
        if (response !is ColorChosenResponse) {
            return ExecutionResult.error(state, "Expected color choice for tapped-for-mana bonus")
        }
        val item = continuation.current
        val available = ManaColorSetResolver.resolve(
            ManaColorSet.AnyColor, state, state.projectedState, item.auraId, item.controllerId, cardRegistry
        )
        val added = manaExecutor.addManaToPool(state, bonusEffect(item.amount), contextFor(state, item.auraId, item.controllerId), response.color, available)
        val driveResult = drive(added.state, continuation.remaining, added.events.toList())
        if (driveResult.isPaused) return driveResult
        return checkForMore(driveResult.state, driveResult.events)
    }

    private fun bonusEffect(amount: Int): AddManaOfChoiceEffect =
        AddManaOfChoiceEffect(colorSet = ManaColorSet.AnyColor, amount = DynamicAmount.Fixed(amount))

    private fun contextFor(state: GameState, auraId: EntityId, controllerId: EntityId): EffectContext =
        EffectContext(
            sourceId = auraId,
            controllerId = controllerId,
            targets = emptyList(),
            xValue = null,
        )
}
