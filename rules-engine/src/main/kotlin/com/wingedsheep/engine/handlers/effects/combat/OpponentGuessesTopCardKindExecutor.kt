package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ChooseGuessKindContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.OpponentGuessesTopCardKindEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [OpponentGuessesTopCardKindEffect] (Gollum, Scheming Guide).
 *
 * Step 1 of the opponent-guess flow: present the framing "land or nonland" choice to the
 * [OpponentGuessesTopCardKindEffect.chooser] (controller by default). The opponent's guess and the
 * reveal/compare/branch happen on resume in [com.wingedsheep.engine.handlers.continuations.GuessContinuationResumer].
 *
 * "your library" is the chooser's library — the player who owns the framing choice. We capture that
 * player as the library owner so the reveal and comparison read the correct top card.
 */
class OpponentGuessesTopCardKindExecutor : EffectExecutor<OpponentGuessesTopCardKindEffect> {

    override val effectType: KClass<OpponentGuessesTopCardKindEffect> =
        OpponentGuessesTopCardKindEffect::class

    override fun execute(
        state: GameState,
        effect: OpponentGuessesTopCardKindEffect,
        context: EffectContext
    ): EffectResult {
        val chooserId = resolveChooser(state, effect.chooser, context)
            ?: return EffectResult.error(state, "No player for OpponentGuessesTopCardKind chooser")
        val guesserId = resolveChooser(state, effect.guesser, context)
            ?: return EffectResult.error(state, "No player for OpponentGuessesTopCardKind guesser")

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = chooserId,
            prompt = "Choose land or nonland",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = listOf("Land", "Nonland")
        )

        val continuation = ChooseGuessKindContinuation(
            decisionId = decisionId,
            controllerLibraryOwnerId = chooserId,
            guesserId = guesserId,
            onGuessedRight = effect.onGuessedRight,
            onGuessedWrong = effect.onGuessedWrong,
            effectContext = context
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = chooserId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    private fun resolveChooser(
        state: GameState,
        chooser: Chooser,
        context: EffectContext
    ): EntityId? = when (chooser) {
        Chooser.Controller -> context.controllerId
        Chooser.Opponent -> state.getOpponents(context.controllerId).firstOrNull()
        Chooser.TargetPlayer -> context.targets.firstOrNull()?.let {
            TargetResolutionUtils.run { it.toEntityId() }
        }
        Chooser.TriggeringPlayer -> context.triggeringEntityId
        Chooser.SourceController -> context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
        }
        else -> null
    }
}
