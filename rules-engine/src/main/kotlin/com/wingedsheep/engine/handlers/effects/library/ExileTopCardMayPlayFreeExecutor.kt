package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CardPlottedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.SourcePlottedOnPriorTurn
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithCostIncreaseEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.MakePlottedEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import kotlin.reflect.KClass

/**
 * Executor for GrantMayPlayFromExileEffect.
 *
 * Registers a [MayPlayPermission] on the game state targeting the cards in the named
 * collection, granting the controller permission to play them from exile. The expiry
 * is encoded on the permission (until end of turn, until a future step, or permanent).
 */
class GrantMayPlayFromExileExecutor : EffectExecutor<GrantMayPlayFromExileEffect> {

    override val effectType: KClass<GrantMayPlayFromExileEffect> = GrantMayPlayFromExileEffect::class

    override fun execute(
        state: GameState,
        effect: GrantMayPlayFromExileEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val collection = context.pipeline.storedCollections[effect.from] ?: emptyList()

        val isPermanent = effect.expiry is MayPlayExpiry.Permanent
        val expiresAfterTurn = expiresAfterTurnFor(state, controllerId, effect.expiry)

        var newState = state
        if (collection.isNotEmpty()) {
            val (permId, stateWithPerm) = newState.newEntity()
            newState = stateWithPerm

            // "When you play a card this way, …" rider (Fires of Mount Doom). Register an
            // event-based delayed triggered ability linked to the permission via a shared id;
            // playing a granted card emits a CardPlayedFromPermissionEvent carrying that id,
            // which fires the rider on the stack. EndOfTurn expiry mirrors the impulse grant's
            // own end-of-turn cleanup.
            val riderLinkId: String? = effect.onPlayRider?.let { rider ->
                val sourceId = context.sourceId ?: return@let null
                val linkId = "may-play-rider-${permId.value}"
                newState = newState.addDelayedTrigger(
                    DelayedTriggeredAbility(
                        id = linkId,
                        effect = rider,
                        sourceId = sourceId,
                        sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "",
                        controllerId = controllerId,
                        trigger = TriggerSpec(EventPattern.CardPlayedFromPermissionEvent),
                        expiry = DelayedTriggerExpiry.EndOfTurn,
                        fireOnce = false,
                    )
                )
                linkId
            }

            newState = newState.addMayPlayPermission(
                MayPlayPermission(
                    id = permId,
                    cardIds = collection.toSet(),
                    controllerId = controllerId,
                    sourceId = context.sourceId,
                    condition = effect.condition,
                    withAnyManaType = effect.withAnyManaType,
                    landEntersTapped = effect.landEntersTapped,
                    permanent = isPermanent,
                    expiresAfterTurn = expiresAfterTurn,
                    riderLinkId = riderLinkId,
                    timestamp = state.timestamp,
                )
            )
        }

        return EffectResult.success(newState)
    }

    /**
     * Translate a [MayPlayExpiry] into the turn whose cleanup will remove the permission.
     * Returns `null` for [MayPlayExpiry.EndOfTurn] (default handling: cleared this cleanup)
     * and for [MayPlayExpiry.Permanent] (component is flagged permanent and skipped).
     */
    private fun expiresAfterTurnFor(
        state: GameState,
        controllerId: EntityId,
        expiry: MayPlayExpiry
    ): Int? = when (expiry) {
        MayPlayExpiry.EndOfTurn, MayPlayExpiry.Permanent -> null
        is MayPlayExpiry.UntilControllerStep -> resolveStepTurn(state, controllerId, expiry)
    }

    /**
     * Resolve which turn's cleanup will mark "the controller's next [step]" given the
     * current step and active player. The cleanup-driven removal is coarse — it runs once
     * per turn at cleanup — so we map any step in the turn to that turn's cleanup.
     *
     * The returned value is a *round* number, because [GameState.turnNumber] is round-based —
     * it increments only when the starting player begins a new turn, so every player's turn
     * within a round shares the same number. The cleanup expiry check disambiguates which
     * player's turn it is by also requiring `activePlayerId == controllerId`; this function
     * therefore only has to name the round in which the controller's next matching turn falls.
     * Counting *player-turns* until the controller's turn (and adding them to a round-based
     * number) over-counts whenever the grant happens on an opponent's turn — the bug that let
     * "until your next end step" leak an extra full turn when a creature died on the opponent's
     * turn (Shadow Urchin).
     */
    private fun resolveStepTurn(
        state: GameState,
        controllerId: EntityId,
        expiry: MayPlayExpiry.UntilControllerStep
    ): Int {
        val turnOrder = state.turnOrder
        val playerIndex = turnOrder.indexOf(controllerId)
        val activeIndex = turnOrder.indexOf(state.activePlayerId)

        val onControllerTurn = playerIndex == activeIndex
        val targetStep = expiry.step
        val targetReachedThisTurn = state.step.ordinal >= targetStep.ordinal
        val thisTurnStillCounts = onControllerTurn && expiry.includeCurrentTurn && !targetReachedThisTurn

        return when {
            // This turn's matching step still counts — expire at this turn's cleanup.
            thisTurnStillCounts -> state.turnNumber
            // Controller's own turn, but this turn no longer counts — their next turn is next round.
            onControllerTurn -> state.turnNumber + 1
            // Opponent's turn: the controller still takes a turn *this* round if they come later in
            // turn order; otherwise they already went and their next turn is in the next round.
            playerIndex > activeIndex -> state.turnNumber
            else -> state.turnNumber + 1
        }
    }
}

/**
 * Executor for [MakePlottedEffect] (CR 718).
 *
 * Plots every card in the named collection. The cards are already in exile (a preceding
 * pipeline step moved them there). For each card this stamps a [PlottedComponent] +
 * [PlayWithoutPayingCostComponent] (permanent) and registers a permanent [MayPlayPermission]
 * gated by [SourcePlottedOnPriorTurn], reproducing the state the [PlotCardHandler] special
 * action produces — minus the plot-cost payment, which the Plot keyword charges and an
 * effect-driven plot (e.g. Make Your Own Luck) does not. Emits a [CardPlottedEvent] per card
 * so "when this card becomes plotted" triggers fire (TriggerDetector.detectPlottedCardTriggers).
 *
 * The controller of the plotted card (who may later cast it for free) is the effect's
 * controller, matching CR 718.2 — the player who caused the card to become plotted.
 */
class MakePlottedExecutor : EffectExecutor<MakePlottedEffect> {

    override val effectType: KClass<MakePlottedEffect> = MakePlottedEffect::class

    override fun execute(
        state: GameState,
        effect: MakePlottedEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val collection = context.pipeline.storedCollections[effect.from] ?: emptyList()
        if (collection.isEmpty()) return EffectResult.success(state)

        var newState = state
        val events = mutableListOf<GameEvent>()
        for (cardId in collection) {
            val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
            val cardName = cardComponent?.name ?: "card"
            // CR 718.2: a plotted card's owner is the player who may later cast it for free.
            // For "you plot a card you own" effects the owner is the controller anyway; for
            // "exile target spell, it becomes plotted" (Aven Interrupter) the spell's owner —
            // not the player who plotted it — gets the free-cast permission.
            val plottedController =
                if (effect.ownerControls) (cardComponent?.ownerId ?: controllerId) else controllerId
            newState = newState.updateEntity(cardId) { container ->
                container
                    .with(PlottedComponent(controllerId = plottedController, turnPlotted = newState.turnNumber))
                    .with(PlayWithoutPayingCostComponent(controllerId = plottedController, permanent = true))
            }
            val (permId, stateWithPerm) = newState.newEntity()
            newState = stateWithPerm.addMayPlayPermission(
                MayPlayPermission(
                    id = permId,
                    cardIds = setOf(cardId),
                    controllerId = plottedController,
                    sourceId = cardId,
                    condition = SourcePlottedOnPriorTurn,
                    permanent = true,
                    timestamp = newState.timestamp,
                )
            )
            events.add(CardPlottedEvent(plottedController, cardId, cardName))
        }

        return EffectResult.success(newState, events)
    }
}

/**
 * Executor for GrantPlayWithoutPayingCostEffect.
 *
 * Marks all cards in a named collection with PlayWithoutPayingCostComponent,
 * allowing the controller to play them without paying mana cost until end of turn.
 */
class GrantPlayWithoutPayingCostExecutor : EffectExecutor<GrantPlayWithoutPayingCostEffect> {

    override val effectType: KClass<GrantPlayWithoutPayingCostEffect> = GrantPlayWithoutPayingCostEffect::class

    override fun execute(
        state: GameState,
        effect: GrantPlayWithoutPayingCostEffect,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        val collection = context.pipeline.storedCollections[effect.from] ?: emptyList()

        var newState = state
        for (cardId in collection) {
            newState = newState.updateEntity(cardId) { container ->
                container.with(PlayWithoutPayingCostComponent(controllerId = controllerId))
            }
        }

        return EffectResult.success(newState)
    }
}

/**
 * Executor for GrantPlayWithCostIncreaseEffect.
 *
 * Stamps PlayWithCostIncreaseComponent on every card in the named collection so that
 * when [context.controllerId] later casts the card, [GrantPlayWithCostIncreaseEffect.amount]
 * generic mana is added to the spell's cost. Mirrors [GrantPlayWithoutPayingCostExecutor]
 * but pushes a cost upwards rather than waiving it.
 */
class GrantPlayWithCostIncreaseExecutor : EffectExecutor<GrantPlayWithCostIncreaseEffect> {

    override val effectType: KClass<GrantPlayWithCostIncreaseEffect> = GrantPlayWithCostIncreaseEffect::class

    override fun execute(
        state: GameState,
        effect: GrantPlayWithCostIncreaseEffect,
        context: EffectContext
    ): EffectResult {
        if (effect.amount <= 0) return EffectResult.success(state)
        val controllerId = context.controllerId
        val collection = context.pipeline.storedCollections[effect.from] ?: emptyList()

        var newState = state
        for (cardId in collection) {
            newState = newState.updateEntity(cardId) { container ->
                container.with(
                    PlayWithCostIncreaseComponent(
                        controllerId = controllerId,
                        amount = effect.amount
                    )
                )
            }
        }

        return EffectResult.success(newState)
    }
}
