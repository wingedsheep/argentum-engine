package com.wingedsheep.engine.handlers.actions.land

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.engine.legalactions.utils.LandDropUtils
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import kotlin.reflect.KClass

/**
 * Handler for the PlayLand action.
 *
 * Playing a land is a special action that doesn't use the stack.
 * It moves the land from hand to battlefield and uses up a land drop.
 */
class PlayLandHandler(
    private val cardRegistry: CardRegistry,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor
) : ActionHandler<PlayLand> {
    override val actionType: KClass<PlayLand> = PlayLand::class

    override fun validate(state: GameState, action: PlayLand): String? {
        if (state.activePlayerId != action.playerId) {
            return "You can only play lands on your turn"
        }
        if (!state.step.isMainPhase) {
            return "You can only play lands during a main phase"
        }
        if (state.stack.isNotEmpty()) {
            return "You can only play lands when the stack is empty"
        }

        // Check land drop availability (accounts for static ability bonuses)
        val landDrops = state.getEntity(action.playerId)?.get<LandDropsComponent>()
            ?: LandDropsComponent()
        val staticBonus = LandDropUtils.getAdditionalLandDrops(state, action.playerId, cardRegistry)
        if (landDrops.remaining + staticBonus <= 0) {
            return "You have already played a land this turn"
        }

        // Check card exists and is a land
        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        if (!cardComponent.typeLine.isLand) {
            return "You can only play land cards as lands"
        }

        // Check card is in hand, on top of library with PlayFromTopOfLibrary, in exile with MayPlayFromExileComponent,
        // or in graveyard with MayPlayPermanentsFromGraveyard permission (Muldrotha)
        val handZone = ZoneKey(action.playerId, Zone.HAND)
        val inHand = action.cardId in state.getZone(handZone)
        val onTopOfLibrary = !inHand && isOnTopOfLibraryWithPermission(state, action.playerId, action.cardId)
        val mayPlayFromExile = !inHand && !onTopOfLibrary && isInExileWithPlayPermission(state, action.playerId, action.cardId)
        val mayPlayFromGraveyard = !inHand && !onTopOfLibrary && !mayPlayFromExile &&
            isInGraveyardWithPlayPermission(state, action.playerId, action.cardId)
        if (!inHand && !onTopOfLibrary && !mayPlayFromExile && !mayPlayFromGraveyard) {
            return "Land is not in your hand"
        }

        return null
    }

    override fun execute(state: GameState, action: PlayLand): ExecutionResult {
        val container = state.getEntity(action.cardId)
            ?: return ExecutionResult.error(state, "Card not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        var newState = state

        // Remove from hand, library, exile, or graveyard (whichever zone the card is in)
        val handZone = ZoneKey(action.playerId, Zone.HAND)
        val libraryZone = ZoneKey(action.playerId, Zone.LIBRARY)
        val graveyardZone = ZoneKey(action.playerId, Zone.GRAVEYARD)
        // Check all exile zones since cards may be in another player's exile (Villainous Wealth)
        val exileOwner = state.turnOrder.firstOrNull { pid ->
            action.cardId in state.getZone(ZoneKey(pid, Zone.EXILE))
        }
        val fromZone = when {
            action.cardId in state.getZone(handZone) -> Zone.HAND
            action.cardId in state.getZone(libraryZone) -> Zone.LIBRARY
            exileOwner != null -> Zone.EXILE
            action.cardId in state.getZone(graveyardZone) -> Zone.GRAVEYARD
            else -> Zone.HAND
        }
        val sourceZoneKey = if (fromZone == Zone.EXILE && exileOwner != null) {
            ZoneKey(exileOwner, Zone.EXILE)
        } else {
            ZoneKey(action.playerId, fromZone)
        }
        newState = newState.removeFromZone(sourceZoneKey, action.cardId)

        // Record Muldrotha graveyard land permission usage
        if (fromZone == Zone.GRAVEYARD) {
            newState = recordGraveyardPlayPermissionUsage(newState, action.playerId, CardType.LAND.name)
        }

        // Add to battlefield
        val battlefieldZone = ZoneKey(action.playerId, Zone.BATTLEFIELD)
        newState = newState.addToZone(battlefieldZone, action.cardId)

        // Add controller component
        newState = newState.updateEntity(action.cardId) { c ->
            c.with(ControllerComponent(action.playerId))
        }

        // Check for "enters the battlefield tapped" replacement effect
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        if (cardDef != null) {
            val entersTapped = cardDef.script.replacementEffects.filterIsInstance<EntersTapped>().firstOrNull()
            if (entersTapped != null) {
                if (entersTapped.payLifeCost != null) {
                    // Shock land: ask the player if they want to pay life
                    // Use up a land drop first
                    newState = newState.updateEntity(action.playerId) { c ->
                        val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
                        c.with(landDrops.use())
                    }

                    val zoneChangeEvent = com.wingedsheep.engine.core.ZoneChangeEvent(
                        action.cardId,
                        cardComponent.name,
                        fromZone,
                        Zone.BATTLEFIELD,
                        action.playerId
                    )
                    val events = listOf(zoneChangeEvent)
                    newState = newState.tick()

                    val decisionId = "pay-life-or-enter-tapped-${action.cardId.value}"
                    val decision = com.wingedsheep.engine.core.YesNoDecision(
                        id = decisionId,
                        playerId = action.playerId,
                        prompt = "Pay ${entersTapped.payLifeCost} life to have ${cardComponent.name} enter untapped?",
                        context = com.wingedsheep.engine.core.DecisionContext(
                            sourceId = action.cardId,
                            sourceName = cardComponent.name,
                            phase = com.wingedsheep.engine.core.DecisionPhase.RESOLUTION
                        )
                    )
                    val continuation = com.wingedsheep.engine.core.PayLifeOrEnterTappedLandContinuation(
                        decisionId = decisionId,
                        landId = action.cardId,
                        controllerId = action.playerId,
                        lifeCost = entersTapped.payLifeCost!!,
                        fromZone = fromZone
                    )
                    val pausedState = newState
                        .pushContinuation(continuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision, events)
                } else {
                    val shouldEnterTapped = if (entersTapped.unlessCondition != null) {
                        // Conditional: enters tapped UNLESS condition is met
                        val context = EffectContext(
                            sourceId = action.cardId,
                            controllerId = action.playerId,
                            opponentId = newState.turnOrder.firstOrNull { it != action.playerId }
                        )
                        !ConditionEvaluator().evaluate(newState, entersTapped.unlessCondition!!, context)
                    } else {
                        true
                    }
                    if (shouldEnterTapped) {
                        newState = newState.updateEntity(action.cardId) { c ->
                            c.with(TappedComponent)
                        }
                    }
                }
            }
        }

        // Check for "as enters, choose X" replacement effect (color or creature type)
        // Process first choice in priority order: COLOR → CREATURE_TYPE
        // Continuations handle chaining to subsequent choices.
        if (cardDef != null) {
            val firstChoice = cardDef.script.replacementEffects
                .filterIsInstance<EntersWithChoice>()
                .sortedBy { it.choiceType.ordinal }
                .firstOrNull()
            if (firstChoice != null) {
                // Use up a land drop first
                newState = newState.updateEntity(action.playerId) { c ->
                    val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
                    c.with(landDrops.use())
                }

                val zoneChangeEvent = ZoneChangeEvent(
                    action.cardId,
                    cardComponent.name,
                    fromZone,
                    Zone.BATTLEFIELD,
                    action.playerId
                )
                val events = listOf(zoneChangeEvent)
                newState = newState.tick()

                val chooserId = when (firstChoice.chooser) {
                    com.wingedsheep.sdk.scripting.references.Player.Opponent ->
                        newState.turnOrder.firstOrNull { it != action.playerId } ?: action.playerId
                    else -> action.playerId
                }

                val result = when (firstChoice.choiceType) {
                    ChoiceType.COLOR -> {
                        val decisionId = "choose-color-land-enters-${action.cardId.value}"
                        val decision = com.wingedsheep.engine.core.ChooseColorDecision(
                            id = decisionId,
                            playerId = chooserId,
                            prompt = "Choose a color",
                            context = com.wingedsheep.engine.core.DecisionContext(
                                sourceId = action.cardId,
                                sourceName = cardComponent.name,
                                phase = com.wingedsheep.engine.core.DecisionPhase.RESOLUTION
                            )
                        )
                        val continuation = com.wingedsheep.engine.core.EntersWithChoiceLandContinuation(
                            decisionId = decisionId,
                            landId = action.cardId,
                            controllerId = action.playerId,
                            choiceType = ChoiceType.COLOR
                        )
                        val pausedState = newState
                            .pushContinuation(continuation)
                            .withPendingDecision(decision)
                        ExecutionResult.paused(pausedState, decision, events)
                    }

                    ChoiceType.CREATURE_TYPE -> {
                        val allCreatureTypes = com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES
                        val decisionId = "choose-creature-type-land-enters-${action.cardId.value}"
                        val decision = com.wingedsheep.engine.core.ChooseOptionDecision(
                            id = decisionId,
                            playerId = chooserId,
                            prompt = "Choose a creature type",
                            context = com.wingedsheep.engine.core.DecisionContext(
                                sourceId = action.cardId,
                                sourceName = cardComponent.name,
                                phase = com.wingedsheep.engine.core.DecisionPhase.RESOLUTION
                            ),
                            options = allCreatureTypes,
                            defaultSearch = ""
                        )
                        val continuation = com.wingedsheep.engine.core.EntersWithChoiceLandContinuation(
                            decisionId = decisionId,
                            landId = action.cardId,
                            controllerId = action.playerId,
                            choiceType = ChoiceType.CREATURE_TYPE,
                            creatureTypes = allCreatureTypes
                        )
                        val pausedState = newState
                            .pushContinuation(continuation)
                            .withPendingDecision(decision)
                        ExecutionResult.paused(pausedState, decision, events)
                    }

                    ChoiceType.CREATURE_ON_BATTLEFIELD -> {
                        // Lands don't use CREATURE_ON_BATTLEFIELD, but handle gracefully
                        null
                    }
                }
                if (result != null) return result
            }
        }

        // Use up a land drop
        newState = newState.updateEntity(action.playerId) { c ->
            val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
            c.with(landDrops.use())
        }

        val zoneChangeEvent = ZoneChangeEvent(
            action.cardId,
            cardComponent.name,
            fromZone,
            Zone.BATTLEFIELD,
            action.playerId
        )

        val events = listOf(zoneChangeEvent)
        newState = newState.tick()

        // Detect and process any triggers from the land entering (e.g., landfall)
        val triggers = triggerDetector.detectTriggers(newState, events)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(newState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    events + triggerResult.events
                )
            }

            return ExecutionResult.success(
                triggerResult.newState,
                events + triggerResult.events
            )
        }

        return ExecutionResult.success(newState, events)
    }

    private fun isOnTopOfLibraryWithPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val library = state.getLibrary(playerId)
        if (library.isEmpty() || library.first() != cardId) return false
        return hasPlayFromTopOfLibrary(state, playerId)
    }

    private fun isInExileWithPlayPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val inAnyExile = state.turnOrder.any { pid ->
            cardId in state.getZone(ZoneKey(pid, Zone.EXILE))
        }
        if (!inAnyExile) return false
        val component = state.getEntity(cardId)?.get<MayPlayFromExileComponent>()
        return component?.controllerId == playerId
    }

    private fun hasPlayFromTopOfLibrary(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any {
                it is PlayFromTopOfLibrary || it is PlayLandsAndCastFilteredFromTopOfLibrary
            }) {
                return true
            }
        }
        return false
    }

    /**
     * Check if a land card is in the player's graveyard and there's a Muldrotha-like permanent
     * with unused land permission.
     */
    private fun isInGraveyardWithPlayPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        if (cardId !in state.getZone(graveyardZone)) return false
        return findGraveyardPlayPermissionSource(state, playerId, CardType.LAND.name) != null
    }

    /**
     * Find a battlefield permanent controlled by the player that has MayPlayPermanentsFromGraveyard
     * and hasn't used its permission for the given type this turn.
     */
    private fun findGraveyardPlayPermissionSource(
        state: GameState,
        playerId: EntityId,
        typeName: String
    ): EntityId? {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is MayPlayPermanentsFromGraveyard }) {
                val tracker = state.getEntity(entityId)?.get<GraveyardPlayPermissionUsedComponent>()
                if (tracker == null || !tracker.hasUsedType(typeName)) {
                    return entityId
                }
            }
        }
        return null
    }

    /**
     * Record that a Muldrotha-like permanent's graveyard play permission was used for a type.
     */
    private fun recordGraveyardPlayPermissionUsage(
        state: GameState,
        playerId: EntityId,
        typeName: String
    ): GameState {
        val sourceId = findGraveyardPlayPermissionSource(state, playerId, typeName) ?: return state
        return state.updateEntity(sourceId) { c ->
            val tracker = c.get<GraveyardPlayPermissionUsedComponent>() ?: GraveyardPlayPermissionUsedComponent()
            c.with(tracker.withUsedType(typeName))
        }
    }

    companion object {
        fun create(services: EngineServices): PlayLandHandler {
            return PlayLandHandler(services.cardRegistry, services.triggerDetector, services.triggerProcessor)
        }
    }
}
