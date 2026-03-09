package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextReplacement
import com.wingedsheep.engine.state.components.identity.TextReplacementCategory
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.handlers.effects.library.ChooseCreatureTypePipelineExecutor
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration

class CreatureTypeChoiceContinuationResumer(
    private val ctx: ContinuationContext
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ChooseFromCreatureTypeContinuation::class, ::resumeChooseFromCreatureType),
        resumer(ChooseToCreatureTypeContinuation::class, ::resumeChooseToCreatureType),
        resumer(ChooseOptionPipelineContinuation::class, ::resumeChooseOptionPipeline),
        resumer(BecomeCreatureTypeContinuation::class, ::resumeBecomeCreatureType),
        resumer(ChooseCreatureTypeGainControlContinuation::class, ::resumeChooseCreatureTypeGainControl),
        resumer(BecomeChosenTypeAllCreaturesContinuation::class, ::resumeBecomeChosenTypeAllCreatures),
        resumer(EachPlayerChoosesCreatureTypeContinuation::class, ::resumeEachPlayerChoosesCreatureType)
    )

    /**
     * Resume after player chooses the FROM creature type for text replacement.
     * Presents the TO creature type choice (excluding Wall).
     */
    fun resumeChooseFromCreatureType(
        state: GameState,
        continuation: ChooseFromCreatureTypeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val fromType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        // Present TO creature type choice, excluding any types specified by the effect
        val excludedTypes = continuation.excludedTypes.map { it.lowercase() }.toSet()
        val toOptions = Subtype.ALL_CREATURE_TYPES.filter {
            it.lowercase() !in excludedTypes
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        val promptSuffix = if (continuation.excludedTypes.isNotEmpty()) {
            ", can't be ${continuation.excludedTypes.joinToString(" or ")}"
        } else ""
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = continuation.controllerId,
            prompt = "Choose the replacement creature type (replacing $fromType$promptSuffix)",
            context = DecisionContext(
                sourceId = continuation.sourceId,
                sourceName = continuation.sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = toOptions
        )

        val nextContinuation = ChooseToCreatureTypeContinuation(
            decisionId = decisionId,
            controllerId = continuation.controllerId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            targetId = continuation.targetId,
            fromType = fromType,
            creatureTypes = toOptions
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(nextContinuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = continuation.controllerId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Resume after player chooses the TO creature type for text replacement.
     * Applies the TextReplacementComponent to the target entity.
     */
    fun resumeChooseToCreatureType(
        state: GameState,
        continuation: ChooseToCreatureTypeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val toType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val targetId = continuation.targetId

        // Target must still exist
        if (state.getEntity(targetId) == null) {
            return checkForMore(state, emptyList())
        }

        // Add or update TextReplacementComponent on the target
        val existingComponent = state.getEntity(targetId)
            ?.get<TextReplacementComponent>()

        val replacement = TextReplacement(
            fromWord = continuation.fromType,
            toWord = toType,
            category = TextReplacementCategory.CREATURE_TYPE
        )

        val newComponent = existingComponent?.withReplacement(replacement)
            ?: TextReplacementComponent(
                replacements = listOf(replacement)
            )

        val newState = state.updateEntity(targetId) { container ->
            container.with(newComponent)
        }

        return checkForMore(newState, emptyList())
    }

    /**
     * Resume after player chose a generic option in a pipeline context.
     *
     * Stores the chosen value into the EffectContinuation below on the stack
     * (via chosenValues map) so subsequent pipeline effects can access it
     * via EffectContext.chosenValues[storeAs].
     *
     * Special case: when storeAs == "chosenCreatureType", additionally injects
     * into the dedicated chosenCreatureType field for backward compatibility
     * with RevealUntilExecutor and SelectFromCollectionExecutor.
     */
    fun resumeChooseOptionPipeline(
        state: GameState,
        continuation: ChooseOptionPipelineContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for generic option selection")
        }

        val chosenValue = continuation.options.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid option index: ${response.optionIndex}")

        // Inject the chosen value into the next EffectContinuation on the stack
        val nextFrame = state.peekContinuation()
        val newState = if (nextFrame is EffectContinuation) {
            val (_, stateAfterPop) = state.popContinuation()
            val updatedFrame = if (continuation.storeAs == ChooseCreatureTypePipelineExecutor.CHOSEN_CREATURE_TYPE_KEY) {
                // Store into dedicated field for downstream executors that read context.chosenCreatureType,
                // and also into chosenValues so pipeline effects (e.g., GroupFilter.chosenSubtypeKey) can read it
                nextFrame.copy(
                    chosenCreatureType = chosenValue,
                    chosenValues = nextFrame.chosenValues + (continuation.storeAs to chosenValue)
                )
            } else {
                nextFrame.copy(chosenValues = nextFrame.chosenValues + (continuation.storeAs to chosenValue))
            }
            stateAfterPop.pushContinuation(updatedFrame)
        } else {
            state
        }

        return checkForMore(newState, emptyList())
    }

    /**
     * Resume after player chose a creature type for a "becomes the creature type
     * of your choice" effect. Creates a floating effect that replaces all creature
     * subtypes with the chosen type.
     */
    fun resumeBecomeCreatureType(
        state: GameState,
        continuation: BecomeCreatureTypeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val targetId = continuation.targetId

        // Target must still be on the battlefield
        if (targetId !in state.getBattlefield()) {
            return checkForMore(state, emptyList())
        }

        val targetCard = state.getEntity(targetId)?.get<CardComponent>()
        val targetName = targetCard?.name ?: "creature"

        // Create a floating effect that sets creature subtypes
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.TYPE,
                sublayer = null,
                modification = SerializableModification.SetCreatureSubtypes(
                    subtypes = setOf(chosenType)
                ),
                affectedEntities = setOf(targetId)
            ),
            duration = continuation.duration,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        val events = listOf(
            CreatureTypeChangedEvent(
                targetId = targetId,
                targetName = targetName,
                newType = chosenType,
                sourceName = continuation.sourceName ?: "Unknown"
            )
        )

        return checkForMore(newState, events)
    }

    /**
     * Resume after player chose a creature type for Peer Pressure-style gain control.
     *
     * If the controller has strictly more creatures of the chosen type than each other
     * player, gains control of all creatures of that type.
     */
    fun resumeChooseCreatureTypeGainControl(
        state: GameState,
        continuation: ChooseCreatureTypeGainControlContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val result = applyChooseCreatureTypeGainControl(
            state = state,
            chosenType = chosenType,
            controllerId = continuation.controllerId,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            duration = continuation.duration
        )

        return if (result.isSuccess) {
            checkForMore(result.state, result.events)
        } else {
            result
        }
    }

    /**
     * Resume after player chose a creature type for global type change.
     *
     * Sets all creatures on the battlefield to the chosen type via a floating effect.
     */
    fun resumeBecomeChosenTypeAllCreatures(
        state: GameState,
        continuation: BecomeChosenTypeAllCreaturesContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        // Find creatures on the battlefield (optionally filtered by controller)
        val affectedEntities = mutableSetOf<EntityId>()
        val events = mutableListOf<GameEvent>()
        val projected = if (continuation.controllerOnly) state.projectedState else null

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Check if creature (face-down permanents are always creatures per Rule 707.2)
            if (!cardComponent.typeLine.isCreature && !container.has<FaceDownComponent>()) continue

            // If controllerOnly, only affect creatures the controller controls
            if (continuation.controllerOnly && projected != null) {
                val controller = projected.getController(entityId)
                if (controller != continuation.controllerId) continue
            }

            affectedEntities.add(entityId)
            events.add(
                CreatureTypeChangedEvent(
                    targetId = entityId,
                    targetName = cardComponent.name,
                    newType = chosenType,
                    sourceName = continuation.sourceName ?: "Unknown"
                )
            )
        }

        if (affectedEntities.isEmpty()) {
            return checkForMore(state, emptyList())
        }

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.TYPE,
                sublayer = null,
                modification = SerializableModification.SetCreatureSubtypes(
                    subtypes = setOf(chosenType)
                ),
                affectedEntities = affectedEntities
            ),
            duration = continuation.duration,
            sourceId = continuation.sourceId,
            sourceName = continuation.sourceName,
            controllerId = continuation.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return checkForMore(newState, events)
    }

    /**
     * Resume after a player chose a creature type for "each player chooses a creature type" effects.
     *
     * Records the chosen type, asks the next player if any remain,
     * or injects the chosen types into the EffectContinuation below on the stack
     * via storedStringLists[storeAs].
     */
    fun resumeEachPlayerChoosesCreatureType(
        state: GameState,
        continuation: EachPlayerChoosesCreatureTypeContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice response for creature type selection")
        }

        val chosenType = continuation.creatureTypes.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid creature type index: ${response.optionIndex}")

        val updatedChosenTypes = continuation.chosenTypes + chosenType

        // If there are more players, ask the next one
        if (continuation.remainingPlayers.isNotEmpty()) {
            val nextPlayer = continuation.remainingPlayers.first()
            val nextRemaining = continuation.remainingPlayers.drop(1)

            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = ChooseOptionDecision(
                id = decisionId,
                playerId = nextPlayer,
                prompt = "Choose a creature type",
                context = DecisionContext(
                    sourceId = continuation.sourceId,
                    sourceName = continuation.sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                options = continuation.creatureTypes
            )

            val newContinuation = continuation.copy(
                decisionId = decisionId,
                currentPlayerId = nextPlayer,
                remainingPlayers = nextRemaining,
                chosenTypes = updatedChosenTypes
            )

            val stateWithDecision = state.withPendingDecision(decision)
            val stateWithContinuation = stateWithDecision.pushContinuation(newContinuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = nextPlayer,
                        decisionType = "CHOOSE_OPTION",
                        prompt = decision.prompt
                    )
                )
            )
        }

        // All players have chosen — inject chosen types into EffectContinuation below
        val nextFrame = state.peekContinuation()
        val newState = if (nextFrame is EffectContinuation) {
            val (_, stateAfterPop) = state.popContinuation()
            stateAfterPop.pushContinuation(
                nextFrame.copy(
                    storedStringLists = nextFrame.storedStringLists + (continuation.storeAs to updatedChosenTypes)
                )
            )
        } else {
            state
        }

        return checkForMore(newState, emptyList())
    }

    companion object {

        fun applyChooseCreatureTypeGainControl(
            state: GameState,
            chosenType: String,
            controllerId: EntityId,
            sourceId: EntityId?,
            sourceName: String?,
            duration: Duration
        ): ExecutionResult {
            val projected = state.projectedState

            // Count creatures of the chosen type per player
            val counts = state.turnOrder.associateWith { playerId ->
                state.getBattlefield().count { entityId ->
                    val container = state.getEntity(entityId) ?: return@count false
                    val cardComponent = container.get<CardComponent>() ?: return@count false
                    val isCreature = cardComponent.typeLine.isCreature || container.has<FaceDownComponent>()
                    if (!isCreature) return@count false
                    val controlledBy = container.get<ControllerComponent>()?.playerId
                    controlledBy == playerId && projected.hasSubtype(entityId, chosenType)
                }
            }

            val controllerCount = counts[controllerId] ?: 0
            if (controllerCount == 0) {
                return ExecutionResult.success(state, emptyList())
            }

            // Check if controller has strictly more than EACH other player
            val otherPlayerCounts = counts.filter { it.key != controllerId }
            val controllerHasMore = otherPlayerCounts.all { controllerCount > it.value }

            if (!controllerHasMore) {
                return ExecutionResult.success(state, emptyList())
            }

            // Gain control of all creatures of that type not already controlled by the controller
            var newState = state
            val events = mutableListOf<GameEvent>()

            for (entityId in state.getBattlefield()) {
                val container = newState.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue
                val isCreature = cardComponent.typeLine.isCreature || container.has<FaceDownComponent>()
                if (!isCreature) continue
                if (!projected.hasSubtype(entityId, chosenType)) continue

                val currentControllerId = container.get<ControllerComponent>()?.playerId
                if (currentControllerId == controllerId) continue

                // Remove any previous Layer.CONTROL floating effects from the same source on the same target
                val filteredEffects = newState.floatingEffects.filter { floating ->
                    !(floating.sourceId == sourceId &&
                      floating.effect.layer == Layer.CONTROL &&
                      entityId in floating.effect.affectedEntities)
                }

                val floatingEffect = ActiveFloatingEffect(
                    id = EntityId.generate(),
                    effect = FloatingEffectData(
                        layer = Layer.CONTROL,
                        sublayer = null,
                        modification = SerializableModification.ChangeController(controllerId),
                        affectedEntities = setOf(entityId)
                    ),
                    duration = duration,
                    sourceId = sourceId,
                    sourceName = sourceName,
                    controllerId = controllerId,
                    timestamp = System.currentTimeMillis()
                )

                newState = newState.copy(
                    floatingEffects = filteredEffects + floatingEffect
                )

                events.add(
                    ControlChangedEvent(
                        permanentId = entityId,
                        permanentName = cardComponent.name,
                        oldControllerId = currentControllerId ?: controllerId,
                        newControllerId = controllerId
                    )
                )
            }

            return ExecutionResult.success(newState, events)
        }
    }
}
