package com.wingedsheep.rulesengine.ecs.replacement

import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.action.GameActionEvent

/**
 * Resolves replacement effects by applying them to events before they are finalized.
 *
 * This implements MTG Rule 614's replacement effect pipeline:
 *
 * 1. When an event would occur, check if any replacement effects apply
 * 2. If multiple replacement effects apply, the affected player/controller chooses which to apply first
 * 3. Each replacement effect can only apply once to a given event
 * 4. After applying a replacement, check again for applicable effects
 * 5. Continue until no more replacements apply
 *
 * Key rules:
 * - Rule 614.5: A replacement effect can only apply once per event (no infinite loops)
 * - Rule 614.6: If multiple replacement effects would apply, the affected player chooses the order
 * - Rule 614.7: Prevention effects are a subset of replacement effects
 * - Rule 614.8: Regeneration is a replacement effect
 * - Rule 614.16: Some replacement effects apply "as [X] enters the battlefield"
 *
 * Usage:
 * ```kotlin
 * val resolver = ReplacementEffectResolver()
 * val finalEvents = resolver.applyReplacements(state, events, activeEffects)
 * ```
 */
class ReplacementEffectResolver {

    /**
     * Apply all active replacement effects to a list of events.
     *
     * @param state The current game state
     * @param events The events to potentially replace
     * @param effects The active replacement effects
     * @return The final list of events after all replacements
     */
    fun applyReplacements(
        state: GameState,
        events: List<GameActionEvent>,
        effects: List<ReplacementEffect>
    ): ReplacementResolutionResult {
        val finalEvents = mutableListOf<GameActionEvent>()
        val appliedReplacements = mutableListOf<AppliedReplacement>()

        for (event in events) {
            val result = applyReplacementsToEvent(state, event, effects)
            finalEvents.addAll(result.resultingEvents)
            appliedReplacements.addAll(result.appliedReplacements)
        }

        return ReplacementResolutionResult(finalEvents, appliedReplacements)
    }

    /**
     * Apply replacement effects to a single event.
     *
     * Per Rule 614.5, each replacement effect can only apply once per event.
     * We track which effects have been applied to prevent infinite loops.
     */
    private fun applyReplacementsToEvent(
        state: GameState,
        event: GameActionEvent,
        effects: List<ReplacementEffect>
    ): SingleEventResult {
        var currentEvent: GameActionEvent? = event
        val appliedEffects = mutableSetOf<EntityId>()
        val appliedReplacements = mutableListOf<AppliedReplacement>()

        // Keep applying replacements until none apply or event is fully replaced
        while (currentEvent != null) {
            // Find applicable effects that haven't been applied yet
            val applicableEffects = effects.filter { effect ->
                effect.sourceId !in appliedEffects &&
                effect.appliesTo(currentEvent!!, state)
            }

            if (applicableEffects.isEmpty()) {
                // No more applicable effects - we're done
                break
            }

            // For now, apply effects in order (future: let player choose)
            // TODO: Implement player choice per Rule 614.6
            val effectToApply = applicableEffects.first()
            appliedEffects.add(effectToApply.sourceId)

            val result = effectToApply.apply(currentEvent!!, state)

            when (result) {
                is ReplacementResult.Replaced -> {
                    appliedReplacements.add(AppliedReplacement(
                        effectSource = effectToApply.sourceId,
                        description = effectToApply.description,
                        originalEvent = currentEvent!!,
                        resultingEvents = result.newEvents
                    ))

                    // If replaced with multiple or zero events, recursively process each
                    if (result.newEvents.isEmpty()) {
                        return SingleEventResult(emptyList(), appliedReplacements)
                    } else if (result.newEvents.size == 1) {
                        // Continue processing the single replacement event
                        currentEvent = result.newEvents.first()
                    } else {
                        // Multiple replacement events - process each recursively
                        val allResults = result.newEvents.flatMap { newEvent ->
                            val subResult = applyReplacementsToEvent(
                                state,
                                newEvent,
                                effects.filter { it.sourceId !in appliedEffects }
                            )
                            appliedReplacements.addAll(subResult.appliedReplacements)
                            subResult.resultingEvents
                        }
                        return SingleEventResult(allResults, appliedReplacements)
                    }
                }

                is ReplacementResult.Modified -> {
                    appliedReplacements.add(AppliedReplacement(
                        effectSource = effectToApply.sourceId,
                        description = effectToApply.description,
                        originalEvent = currentEvent!!,
                        resultingEvents = listOf(result.modifiedEvent)
                    ))
                    // Continue with the modified event
                    currentEvent = result.modifiedEvent
                }

                is ReplacementResult.NotApplicable -> {
                    // This effect didn't actually apply, try the next one
                    continue
                }
            }
        }

        return SingleEventResult(
            resultingEvents = currentEvent?.let { listOf(it) } ?: emptyList(),
            appliedReplacements = appliedReplacements
        )
    }

    private data class SingleEventResult(
        val resultingEvents: List<GameActionEvent>,
        val appliedReplacements: List<AppliedReplacement>
    )
}

/**
 * Result of applying replacement effects to events.
 */
data class ReplacementResolutionResult(
    /** The final events after all replacements */
    val events: List<GameActionEvent>,
    /** Record of which replacements were applied */
    val appliedReplacements: List<AppliedReplacement>
)

/**
 * Record of a replacement effect being applied.
 */
data class AppliedReplacement(
    val effectSource: EntityId,
    val description: String,
    val originalEvent: GameActionEvent,
    val resultingEvents: List<GameActionEvent>
)

/**
 * Component to track active replacement effects on the game state.
 *
 * Replacement effects are typically:
 * - Static abilities on permanents (e.g., Hardened Scales)
 * - Continuous effects from resolved spells (e.g., Fog)
 * - State-based (e.g., Rest in Peace)
 */
data class ActiveReplacementEffects(
    val effects: List<ReplacementEffect> = emptyList()
) {
    fun add(effect: ReplacementEffect): ActiveReplacementEffects =
        copy(effects = effects + effect)

    fun remove(sourceId: EntityId): ActiveReplacementEffects =
        copy(effects = effects.filter { it.sourceId != sourceId })

    fun removeAll(predicate: (ReplacementEffect) -> Boolean): ActiveReplacementEffects =
        copy(effects = effects.filterNot(predicate))
}
