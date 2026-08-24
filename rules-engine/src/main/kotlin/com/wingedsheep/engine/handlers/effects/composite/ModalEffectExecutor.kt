package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.modal.ChosenModeMemory
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
 * Handles "Choose one —" / "Choose two —" modal spells and modal triggered / activated
 * abilities.
 *
 * Two paths, dispatched on whether the mode was picked before resolution:
 *
 * - **Pre-chosen modes** (modal spells, rules 700.2 / 601.2b–c): every modal *spell*
 *   reaches this executor with [SpellOnStackComponent.chosenModes] populated —
 *   [com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler] runs the
 *   cast-time mode + per-mode target picker (`pauseForCastTimeModeSelection` →
 *   `presentCastModalTargetDecision`) before the spell ever lands on the stack.
 *   This branch then drains each chosen mode in order with its captured targets,
 *   pausing if a sub-effect needs another decision; remaining modes ride along on a
 *   [ModalPreChosenContinuation] that auto-resumes once the inner decision resolves.
 *   Per-mode Rule 608.2b re-validation is applied against
 *   [SpellOnStackComponent.modeTargetRequirements].
 *
 * - **Resolution-time mode picking**: whatever arrives here with `chosenModes` empty. The executor
 *   presents a [ChooseOptionDecision] inline, pushes [ModalContinuation], and the modal-and-clone
 *   resumer drives target selection via `processChosenModeQueue`. Two populations still take this
 *   path:
 *   - modal **activated** abilities, which don't go through the cast pipeline; and
 *   - a [ModalEffect] **nested inside another effect** — inside a gated effect, a reflexive
 *     trigger, a pipeline step — where the mode question isn't the spell's or ability's own.
 *
 *   Modal *triggered* abilities do **not**: their top-level modes and per-mode targets are picked
 *   as the ability is put onto the stack (CR 603.3c / 700.2b, and 603.3d for the targets) by
 *   [com.wingedsheep.engine.event.TriggerProcessor], so they reach this executor pre-chosen just
 *   like a cast spell.
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
        // Pre-chosen modes flow: used for both direct spell casts and copies
        // (storm / CopyTargetSpell / chain). Both resolvers populate the modal
        // fields from the appropriate stack component (SpellOnStackComponent for
        // spells, TriggeredAbilityOnStackComponent for copies — 700.2g).
        if (context.chosenModes.isNotEmpty()) {
            return executePreChosenModes(state, effect, context)
        }

        // Mode not pre-chosen — present mode selection decision (triggered/activated
        // modal abilities, rule 603.3c; modal spells always arrive pre-chosen via the
        // cast-time picker in CastSpellHandler).
        val playerId = context.controllerId

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // Resolve "choose up to <DynamicAmount>" at runtime.
        //
        // The floor comes from [ModalEffect.dynamicMinChooseCount] when the card set one, and only
        // falls back to 0 — "you may decline every pick" — when it didn't. Forcing 0 here made the
        // synthetic "Don't choose a mode" option appear on a *mandatory* dynamic modal
        // (Frankenstein's Monster: exactly X counters, one per card exiled), letting the player
        // walk away from picks the card doesn't let them skip. `ModalChooseCounts.forCast` has
        // always read the floor for modal *spells*; this is the resolution-time path that a modal
        // nested inside another effect takes, and it had drifted from it.
        //
        // The ceiling is capped by the mode list only when each mode can be picked once. With
        // [ModalEffect.allowRepeat] the same mode stays on the menu every pick (CR 700.2d), so
        // three modes can absorb any number of picks and capping at `modes.size` would silently
        // shrink X. `forCast` makes the same distinction, so the two paths now agree on both bounds.
        val (effectiveChooseCount, effectiveMinChooseCount) = if (effect.dynamicChooseCount != null) {
            val evaluator = com.wingedsheep.engine.handlers.DynamicAmountEvaluator()
            val raw = evaluator.evaluate(state, effect.dynamicChooseCount!!, context)
            val capped = if (effect.allowRepeat) {
                raw.coerceAtLeast(0)
            } else {
                raw.coerceIn(0, effect.modes.size)
            }
            val floor = effect.dynamicMinChooseCount
                ?.let { evaluator.evaluate(state, it, context).coerceIn(0, capped) }
                ?: 0
            capped to floor
        } else {
            effect.chooseCount to effect.minChooseCount
        }

        // Evaluated cap = 0 → no modes will be chosen; resolve as a no-op success.
        if (effectiveChooseCount == 0) {
            return EffectResult.success(state, emptyList())
        }

        // "Choose one that hasn't been chosen" (Gandalf the Grey — game-scoped) / "…this turn"
        // (Breeches, Eager Pillager — turn-scoped): exclude any mode this source has already
        // chosen, recorded in a per-source memory component. If every mode has been chosen, the
        // ability has no legal mode and does nothing.
        val alreadyChosen = ChosenModeMemory.excludedFor(state, context.sourceId, effect)

        val availableIndices = effect.modes.indices.filter { it !in alreadyChosen }
        if (availableIndices.isEmpty()) {
            return EffectResult.success(state, emptyList())
        }
        val baseOptions = availableIndices.map { effect.modes[it].description }
        // "Choose up to N" — allow declining a mode pick when minChooseCount has
        // already been satisfied (here, before any picks, when minChooseCount = 0).
        val canDecline = effectiveMinChooseCount < effectiveChooseCount
        val modeDescriptions = if (canDecline) baseOptions + DECLINE_MODE_LABEL else baseOptions

        val basePrompt = "Choose a mode for ${sourceName ?: "modal spell"}"
        val prompt = if (effectiveChooseCount > 1) "$basePrompt (1 of $effectiveChooseCount)" else basePrompt

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
            triggeringEntityId = context.triggeringEntityId,
            chooseCount = effectiveChooseCount,
            minChooseCount = effectiveMinChooseCount,
            selectedModeIndices = emptyList(),
            availableIndices = availableIndices,
            allowRepeat = effect.allowRepeat,
            outerTargets = context.targets,
            outerNamedTargets = context.pipeline.namedTargets,
            recordChosenModesOnSource = effect.excludePreviouslyChosenModes,
            recordChosenModesThisTurn = effect.excludeModesChosenThisTurn
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
        context: EffectContext
    ): EffectResult {
        val entries = buildModeEntries(
            effect,
            chosenModes = context.chosenModes,
            modeTargetsOrdered = context.modeTargetsOrdered,
            modeTargetRequirements = context.modeTargetRequirements
        )
        val sourceName = context.sourceId?.let { id -> state.getEntity(id)?.get<CardComponent>()?.name }
        val baseCtx = PreTargetedEffectContext(
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            xValue = context.xValue,
            triggeringEntityId = context.triggeringEntityId
        )
        return processPreTargetedEffectQueue(state, entries, baseCtx, effectExecutor, targetValidator, emptyList())
    }

    companion object {
        /**
         * Label for the synthetic "no mode" option appended when a modal effect
         * allows declining (minChooseCount < chooseCount, e.g., "choose up to one").
         */
        const val DECLINE_MODE_LABEL: String = "Don't choose a mode"

        /** Build the drain queue from pre-chosen modes / targets. */
        fun buildModeEntries(
            effect: ModalEffect,
            chosenModes: List<Int>,
            modeTargetsOrdered: List<List<com.wingedsheep.engine.state.components.stack.ChosenTarget>>,
            modeTargetRequirements: Map<Int, List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>>
        ): List<PreTargetedEffectEntry> {
            return chosenModes.mapIndexed { ordinal, modeIndex ->
                val mode = effect.modes.getOrNull(modeIndex)
                val targets = modeTargetsOrdered.getOrNull(ordinal) ?: emptyList()
                val reqs = modeTargetRequirements[modeIndex]
                    ?: mode?.targetRequirements
                    ?: emptyList()
                PreTargetedEffectEntry(
                    effect = mode?.effect ?: error("Invalid pre-chosen mode index: $modeIndex"),
                    targets = targets,
                    targetRequirements = reqs
                )
            }
        }

        /** Convenience overload reading from a [SpellOnStackComponent]. */
        fun buildModeEntries(effect: ModalEffect, spellOnStack: SpellOnStackComponent): List<PreTargetedEffectEntry> =
            buildModeEntries(
                effect,
                spellOnStack.chosenModes,
                spellOnStack.modeTargetsOrdered,
                spellOnStack.modeTargetRequirements
            )
    }
}

/** Base fields needed to build per-mode [EffectContext]s during pre-chosen mode drainage. */
internal data class PreTargetedEffectContext(
    val controllerId: com.wingedsheep.sdk.model.EntityId,
    val sourceId: com.wingedsheep.sdk.model.EntityId?,
    val sourceName: String?,
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
internal fun processPreTargetedEffectQueue(
    state: GameState,
    entries: List<PreTargetedEffectEntry>,
    ctx: PreTargetedEffectContext,
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
            sourceId = ctx.sourceId,
            // This path re-validates a mode whose targets were already chosen and already checked
            // at cast/activation time, and the effect context does not record whether the modal
            // came from a spell or an ability — so it declares ANY rather than guessing. A
            // spell-only restriction is therefore enforced on the way in, not re-enforced here.
            targetingSourceType = TargetingSourceType.ANY
        )
    } else null

    if (validationError != null) {
        // Skip this mode; drain the rest.
        return processPreTargetedEffectQueue(state, tail, ctx, effectExecutor, targetValidator, accumulatedEvents)
    }

    val effectContext = EffectContext(
        sourceId = ctx.sourceId,
        controllerId = ctx.controllerId,
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

    return processPreTargetedEffectQueue(nextState, tail, ctx, effectExecutor, targetValidator, nextEvents)
}
