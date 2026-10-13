package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.continuations.exposeCollectionsToNextFrame
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SecretChoice
import com.wingedsheep.sdk.scripting.effects.SecretChoiceEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

class SecretChoiceExecutor(
    private val decisionHandler: DecisionHandler,
    private val amounts: DynamicAmountEvaluator,
    private val executeEffect: (GameState, Effect, EffectContext) -> EffectResult,
) : EffectExecutor<SecretChoiceEffect> {
    override val effectType: KClass<SecretChoiceEffect> = SecretChoiceEffect::class

    override fun execute(state: GameState, effect: SecretChoiceEffect, context: EffectContext): EffectResult {
        val participants = effect.participants.flatMap {
            TargetResolutionUtils.resolvePlayerTargets(EffectTarget.PlayerRef(it), state, context)
        }.toSet()
        val players = state.apnapOrder.filter { it in participants }
        val choice = effect.choice
        val collections = if (choice is SecretChoice.Entity) mapOf(choice.storeMostChosen to emptyList<EntityId>()) else emptyMap()
        if (players.isEmpty() || (choice is SecretChoice.Entity && context.pipeline.storedCollections[choice.from].isNullOrEmpty()) ||
            (choice is SecretChoice.Number && amounts.evaluate(state, choice.maxValue, context) < choice.minValue)) {
            val result = effect.then?.let { executeEffect(state, it, context.copy(
                pipeline = context.pipeline.copy(storedCollections = context.pipeline.storedCollections + collections)
            )) } ?: EffectResult.success(state)
            return result.copy(updatedCollections = collections + result.updatedCollections)
        }
        return EffectResult.from(SecretChoiceLogic.ask(state, SecretChoiceContinuation(
            effect = effect, effectContext = context, currentPlayerId = players.first(), remainingPlayers = players.drop(1),
            sourceName = context.sourceId?.let { id -> state.getEntity(id)?.get<CardComponent>()?.name
                ?.let { nameVisibleToAll(state, id, it) } },
        ), amounts, decisionHandler))
    }
}

/** Collect first, reveal together, then queue ordinary effect branches so nested questions cannot lose later work. */
object SecretChoiceLogic {
    fun ask(state: GameState, frame: SecretChoiceContinuation, amounts: DynamicAmountEvaluator,
            decisions: DecisionHandler = DecisionHandler()): ExecutionResult {
        val context = frame.effectContext.copy(objectReferences = frame.objectReferences)
        return when (val choice = frame.effect.choice) {
            is SecretChoice.Number -> {
                val max = amounts.evaluate(state, choice.maxValue, context)
                if (max < choice.minValue) return advance(state, frame, amounts)
                decisions.createNumberDecision(state, frame.currentPlayerId, context.sourceId, frame.sourceName,
                    "Secretly choose a number", choice.minValue, max, phase = DecisionPhase.RESOLUTION, answer = frame)
            }
            is SecretChoice.Entity -> decisions.createCardSelectionDecision(
                state, frame.currentPlayerId, context.sourceId, frame.sourceName, "Secretly choose an object",
                options = context.pipeline.storedCollections[choice.from].orEmpty(),
                minSelections = if (choice.optional) 0 else 1, maxSelections = 1,
                useTargetingUI = true, answer = frame,
            )
        }
    }

    fun resume(state: GameState, frame: SecretChoiceContinuation, response: DecisionResponse,
               amounts: DynamicAmountEvaluator): ExecutionResult {
        val answered = when (frame.effect.choice) {
            is SecretChoice.Number -> {
                if (response !is NumberChosenResponse) return ExecutionResult.error(state, "Expected number choice")
                frame.copy(numbers = frame.numbers + (frame.currentPlayerId to response.number))
            }
            is SecretChoice.Entity -> {
                if (response !is CardsSelectedResponse) return ExecutionResult.error(state, "Expected entity choice")
                frame.copy(entities = frame.entities + (frame.currentPlayerId to response.selectedCards.firstOrNull()))
            }
        }.copy(decisionIds = frame.decisionIds + (frame.currentPlayerId to response.decisionId))
        return advance(state, answered, amounts)
    }

    private fun advance(state: GameState, frame: SecretChoiceContinuation, amounts: DynamicAmountEvaluator): ExecutionResult {
        val remaining = frame.remainingPlayers.filter { it in state.activePlayers }
        if (remaining.isNotEmpty()) return ask(state, frame.copy(currentPlayerId = remaining.first(), remainingPlayers = remaining.drop(1)), amounts)
        return finish(state, frame)
    }

    private fun finish(state: GameState, frame: SecretChoiceContinuation): ExecutionResult {
        val original = frame.effectContext.copy(objectReferences = frame.objectReferences)
        val branches = mutableListOf<EffectContinuation>()
        val revealed = mutableListOf<GameEvent>()
        val publicSourceName = frame.sourceName?.let { name ->
            original.sourceId?.let { nameVisibleToAll(state, it, name) } ?: name
        }
        var context = original
        var collections: Map<String, List<EntityId>> = emptyMap()
        when (val choice = frame.effect.choice) {
            is SecretChoice.Number -> {
                frame.numbers.forEach { (player, number) -> revealed += DecisionSubmittedEvent(
                    frame.decisionIds[player].orEmpty(), player, "${publicSourceName?.let { "($it) " }.orEmpty()}Revealed number: $number") }
                val eligible = frame.numbers.filterValues { it >= choice.outcomeMinValue }
                if (eligible.isNotEmpty()) {
                    val highest = eligible.values.max()
                    val lowest = eligible.values.min()
                    val groups = listOfNotNull(
                        choice.highestEffect?.let { it to eligible.filterValues { n -> n == highest } },
                        choice.lowestEffect?.let { it to eligible.filterValues { n -> n == lowest } },
                        choice.tiedEffect?.takeIf { highest == lowest }?.let { it to eligible },
                    )
                    for ((effect, bidders) in groups) for ((player, number) in bidders) branches += EffectContinuation(
                        remainingEffects = listOf(effect), effectContext = original.copy(
                            effectControllerId = original.effectControllerId ?: original.controllerId,
                            controllerId = player, xValue = number))
                }
                val matched = frame.numbers.isNotEmpty() && frame.numbers.values.distinct().size == 1
                (if (matched) choice.matchedEffect else choice.unmatchedEffect)?.let {
                    branches += EffectContinuation(remainingEffects = listOf(it), effectContext = original)
                }
            }
            is SecretChoice.Entity -> {
                frame.entities.forEach { (player, selected) -> revealed += DecisionSubmittedEvent(
                    frame.decisionIds[player].orEmpty(), player,
                    selected?.let { "Revealed choice: ${nameVisibleToAll(state, it, state.getEntity(it)?.get<CardComponent>()?.name ?: "permanent")}" } ?: "Revealed choice: abstain") }
                val votes = frame.entities.values.filterNotNull().groupingBy { it }.eachCount()
                val most = votes.values.maxOrNull()
                val winners = original.pipeline.storedCollections[choice.from].orEmpty().distinct().filter { votes[it] == most && most != null }
                collections = mapOf(choice.storeMostChosen to winners)
                context = original.copy(pipeline = original.pipeline.copy(storedCollections = original.pipeline.storedCollections + collections))
            }
        }
        frame.effect.then?.let { branches += EffectContinuation(remainingEffects = listOf(it), effectContext = context) }
        val published = exposeCollectionsToNextFrame(state, collections)
        val queued = branches.asReversed().fold(published) { s, branch -> s.pushContinuation(branch) }
        return ExecutionResult.success(queued, revealed)
    }
}
