package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ModalEffect.
 * Handles "Choose one —" / "Choose two —" modal spells.
 *
 * Two paths:
 *
 * - **Cast-time modes already chosen** (rules 700.2, 601.2b–c): when the spell's
 *   [SpellOnStackComponent.chosenModes] is non-empty, iterate each chosen mode in
 *   order, executing its effect with the per-mode targets stored on
 *   [SpellOnStackComponent.modeTargetsOrdered]. If a mode's effect pauses for a
 *   nested decision, remaining modes ride along on a
 *   [ModalPreChosenContinuation] that auto-resumes after the inner decision
 *   resolves. Per-mode Rule 608.2b re-validation is applied against
 *   [SpellOnStackComponent.modeTargetRequirements].
 *
 * - **Resolution-time mode picking** (legacy): used for modal triggered
 *   abilities (rule 603.3c) and any other modal whose `chosenModes` is empty.
 *   Presents a ChooseOptionDecision, pushes [ModalContinuation], and the
 *   modal-and-clone resumer drives target selection via `processChosenModeQueue`.
 *
 * @param effectExecutor Function to execute a sub-effect (provided by registry)
 */
class ModalEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<ModalEffect> {

    override val effectType: KClass<ModalEffect> = ModalEffect::class

    private val targetValidator = TargetValidator()

    override fun execute(
        state: GameState,
        effect: ModalEffect,
        context: EffectContext
    ): EffectResult {
        val spellOnStack = context.sourceId?.let { state.getEntity(it)?.get<SpellOnStackComponent>() }
        if (spellOnStack != null && spellOnStack.chosenModes.isNotEmpty()) {
            return executePreChosenModes(state, effect, context, spellOnStack)
        }

        // Mode not pre-chosen — present mode selection decision (legacy flow for
        // triggered/activated modal abilities).
        val playerId = context.controllerId

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val modeDescriptions = effect.modes.map { it.description }
        val availableIndices = effect.modes.indices.toList()

        val basePrompt = "Choose a mode for ${sourceName ?: "modal spell"}"
        val prompt = if (effect.chooseCount > 1) "$basePrompt (1 of ${effect.chooseCount})" else basePrompt

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = playerId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = modeDescriptions
        )

        // Preserve outer-scope targets so no-target modes can resolve ContextTarget
        // references to targets chosen by the enclosing spell/ability (e.g.,
        // Manifold Mouse's BeginCombat trigger targets a Mouse, then picks a
        // keyword mode that grants the keyword to that outer target).
        val continuation = ModalContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            modes = effect.modes,
            xValue = context.xValue,
            opponentId = context.opponentId,
            triggeringEntityId = context.triggeringEntityId,
            chooseCount = effect.chooseCount,
            selectedModeIndices = emptyList(),
            availableIndices = availableIndices,
            outerTargets = context.targets,
            outerNamedTargets = context.pipeline.namedTargets
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
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Iterate each pre-chosen mode in order, executing its effect with per-mode targets.
     * Called synchronously on the first invocation and by the auto-resumer for each
     * remaining mode after a mode's effect pauses.
     */
    private fun executePreChosenModes(
        state: GameState,
        effect: ModalEffect,
        context: EffectContext,
        spellOnStack: SpellOnStackComponent
    ): EffectResult {
        val entries = buildModeEntries(effect, spellOnStack)
        val sourceName = context.sourceId?.let { id -> state.getEntity(id)?.get<CardComponent>()?.name }
        val baseCtx = ModalPreChosenBaseContext(
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            opponentId = context.opponentId,
            xValue = context.xValue,
            triggeringEntityId = context.triggeringEntityId
        )
        return processPreChosenModeQueue(state, entries, baseCtx, effectExecutor, targetValidator, emptyList())
    }

    companion object {
        /** Build the drain queue from the spell's chosenModes / modeTargetsOrdered. */
        fun buildModeEntries(effect: ModalEffect, spellOnStack: SpellOnStackComponent): List<ModalPreChosenEntry> {
            return spellOnStack.chosenModes.mapIndexed { ordinal, modeIndex ->
                val mode = effect.modes.getOrNull(modeIndex)
                val targets = spellOnStack.modeTargetsOrdered.getOrNull(ordinal) ?: emptyList()
                val reqs = spellOnStack.modeTargetRequirements[modeIndex]
                    ?: mode?.targetRequirements
                    ?: emptyList()
                ModalPreChosenEntry(
                    effect = mode?.effect ?: error("Invalid pre-chosen mode index: $modeIndex"),
                    targets = targets,
                    targetRequirements = reqs
                )
            }
        }
    }
}

/** Base fields needed to build per-mode [EffectContext]s during pre-chosen mode drainage. */
internal data class ModalPreChosenBaseContext(
    val controllerId: com.wingedsheep.sdk.model.EntityId,
    val sourceId: com.wingedsheep.sdk.model.EntityId?,
    val sourceName: String?,
    val opponentId: com.wingedsheep.sdk.model.EntityId?,
    val xValue: Int?,
    val triggeringEntityId: com.wingedsheep.sdk.model.EntityId?
)

/**
 * Process the remaining pre-chosen modes of a choose-N modal spell.
 *
 * Synchronously executes each entry's effect in order, applying per-mode 608.2b
 * re-validation against the original target requirements. When a mode's
 * execution pauses, pushes a [ModalPreChosenContinuation] holding the tail and
 * surfaces the pause; the continuation is auto-resumed once the inner decision
 * resolves.
 *
 * Shared between [ModalEffectExecutor] (initial entry) and the auto-resumer for
 * [ModalPreChosenContinuation].
 */
internal fun processPreChosenModeQueue(
    state: GameState,
    entries: List<ModalPreChosenEntry>,
    ctx: ModalPreChosenBaseContext,
    effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    targetValidator: TargetValidator,
    accumulatedEvents: List<GameEvent>
): EffectResult {
    if (entries.isEmpty()) return EffectResult.success(state, accumulatedEvents)

    val head = entries.first()
    val tail = entries.drop(1)

    // 608.2b per-mode fizzle: if the mode required targets and at least one is now illegal,
    // skip the mode entirely. Partial per-target filtering is a future refinement — for now
    // we mirror the all-or-nothing shape used by the resolution-time ModalContinuation path.
    val cardComponent = ctx.sourceId?.let { state.getEntity(it)?.get<CardComponent>() }
    val sourceColors = cardComponent?.colors ?: emptySet()
    val sourceSubtypes = cardComponent?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()

    val validationError = if (head.targetRequirements.isNotEmpty()) {
        targetValidator.validateTargets(
            state = state,
            targets = head.targets,
            requirements = head.targetRequirements,
            casterId = ctx.controllerId,
            sourceColors = sourceColors,
            sourceSubtypes = sourceSubtypes,
            sourceId = ctx.sourceId
        )
    } else null

    if (validationError != null) {
        // Skip this mode; drain the rest.
        return processPreChosenModeQueue(state, tail, ctx, effectExecutor, targetValidator, accumulatedEvents)
    }

    val effectContext = EffectContext(
        sourceId = ctx.sourceId,
        controllerId = ctx.controllerId,
        opponentId = ctx.opponentId,
        xValue = ctx.xValue,
        targets = head.targets,
        pipeline = PipelineState(
            namedTargets = EffectContext.buildNamedTargets(head.targetRequirements, head.targets)
        ),
        triggeringEntityId = ctx.triggeringEntityId
    )

    // Pre-push the tail continuation so that if the effect pauses, our frame sits
    // beneath the inner decision's frames and auto-resumes when they finish.
    val stateForExecution = if (tail.isNotEmpty()) {
        state.pushContinuation(
            ModalPreChosenContinuation(
                decisionId = "modal-pre-chosen-${UUID.randomUUID()}",
                controllerId = ctx.controllerId,
                sourceId = ctx.sourceId,
                sourceName = ctx.sourceName,
                opponentId = ctx.opponentId,
                xValue = ctx.xValue,
                triggeringEntityId = ctx.triggeringEntityId,
                remainingEntries = tail
            )
        )
    } else state

    val result = effectExecutor(stateForExecution, head.effect, effectContext)
    val nextEvents = accumulatedEvents + result.events

    if (result.isPaused) {
        return EffectResult.paused(result.state, result.pendingDecision!!, nextEvents)
    }
    if (result.error != null) {
        return EffectResult(state = result.state, events = nextEvents, error = result.error)
    }

    // Success — pop the pre-pushed tail continuation and drain the rest synchronously.
    val nextState = if (tail.isNotEmpty()) {
        val (_, afterPop) = result.state.popContinuation()
        afterPop
    } else result.state

    return processPreChosenModeQueue(nextState, tail, ctx, effectExecutor, targetValidator, nextEvents)
}
