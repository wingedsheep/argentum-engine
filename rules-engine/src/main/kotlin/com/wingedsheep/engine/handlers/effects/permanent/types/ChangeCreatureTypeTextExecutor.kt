package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ChangeCreatureTypeTextEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ChangeCreatureTypeTextEffect.
 *
 * "Change the text of target spell or permanent by replacing all instances of one
 * creature type with another."
 *
 * This executor:
 * 1. Resolves the target entity
 * 2. Presents a ChooseOptionDecision with all creature types for the FROM type
 * 3. Pushes a ChooseFromCreatureTypeContinuation (with excludedTypes) for the next step
 *
 * The ContinuationHandler handles steps 2 and 3:
 * - ChooseFromCreatureTypeContinuation: presents TO type choice (excluding types from effect)
 * - ChooseToCreatureTypeContinuation: applies the TextReplacementComponent
 */
class ChangeCreatureTypeTextExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ChangeCreatureTypeTextEffect> {

    override val effectType: KClass<ChangeCreatureTypeTextEffect> =
        ChangeCreatureTypeTextEffect::class

    override fun execute(
        state: GameState,
        effect: ChangeCreatureTypeTextEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state.tick())

        // Target must still exist (on battlefield or stack)
        if (targetId !in state.getBattlefield() && targetId !in state.stack) {
            return EffectResult.success(state.tick())
        }

        val allCreatureTypes = Subtype.ALL_CREATURE_TYPES
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val targetCard = state.getEntity(targetId)?.get<CardComponent>()
        val targetName = targetCard?.name

        // Creature types that appear in the target's text — the ones a replacement will visibly
        // affect — are exactly what Artificial Evolution changes ("all instances of one creature
        // type"). That's the card's (projected) subtypes PLUS any creature type named in its rules
        // text (e.g. "Beast" in Wirewood Savage's trigger, which the trigger resolver rewrites).
        // Surface those first and label them "On <card>" so players don't pick a type that isn't
        // there (a common no-op mistake), while still allowing any type.
        val textWords = oracleWords(targetCard?.oracleText)
        val presentTypes = allCreatureTypes.filter {
            it in state.projectedState.getSubtypes(targetId) || it.lowercase() in textWords
        }.toSet()
        val fromOptions = allCreatureTypes.filter { it in presentTypes } +
            allCreatureTypes.filter { it !in presentTypes }
        val fromMetadata = fromOptions.map { type ->
            if (type in presentTypes && targetName != null) OptionMetadata(id = type, description = "On $targetName")
            else OptionMetadata(id = type)
        }
        // Pre-select the target's first on-card creature type.
        val defaultFromIndex = fromOptions.indexOfFirst { it in presentTypes }.takeIf { it >= 0 }

        // TO options: any creature type the effect doesn't forbid (e.g. Artificial Evolution can't
        // pick Wall). No per-from constraint — any allowed type may replace any creature type.
        val excluded = effect.excludedTypes.map { it.lowercase() }.toSet()
        val toOptions = allCreatureTypes.filter { it.lowercase() !in excluded }

        val excludedNote = if (effect.excludedTypes.isNotEmpty())
            " (can't be ${effect.excludedTypes.joinToString(" or ")})" else ""

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseReplacementDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = (if (targetName != null) "Change a creature type in $targetName's text" else "Change a creature type") + excludedNote,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            fromOptions = fromOptions,
            toOptions = toOptions,
            fromMetadata = fromMetadata,
            defaultFromIndex = defaultFromIndex
        )

        val continuation = ChooseReplacementContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            targetId = targetId,
            fromOptions = fromOptions,
            toOptions = toOptions,
            mode = ReplacementMode.CREATURE_TYPE,
            duration = Duration.Permanent
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = context.controllerId,
                    decisionType = "CHOOSE_REPLACEMENT",
                    prompt = decision.prompt
                )
            )
        )
    }
}
