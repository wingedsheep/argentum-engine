package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.ChooserResolution
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ChoosePileEffect
import kotlin.reflect.KClass

/**
 * Executor for [ChoosePileEffect].
 *
 * Reads two named card collections from the pipeline, presents a binary
 * [ChooseOptionDecision] to the player [ChooserResolution] derives from the effect's
 * [com.wingedsheep.sdk.scripting.effects.Chooser], and (on response) routes the picked pile to
 * [ChoosePileEffect.storeChosenAs] and the other to [ChoosePileEffect.storeOtherAs].
 */
class ChoosePileExecutor : EffectExecutor<ChoosePileEffect> {

    override val effectType: KClass<ChoosePileEffect> = ChoosePileEffect::class

    override fun execute(
        state: GameState,
        effect: ChoosePileEffect,
        context: EffectContext
    ): EffectResult {
        val pileA = context.pipeline.storedCollections[effect.pileA] ?: emptyList()
        val pileB = context.pipeline.storedCollections[effect.pileB] ?: emptyList()

        val deciderId = when (
            val outcome = ChooserResolution.resolve(state, effect.chooser, context, pileA + pileB)
        ) {
            is ChooserResolution.Outcome.Resolved -> outcome.playerId
            // "An opponent chooses one of those piles" — with several opponents the controller
            // says which one, then this same effect runs again for the pile choice itself.
            is ChooserResolution.Outcome.NeedsOpponentPick -> return ChooserResolution.pauseForOpponentPick(
                state, outcome.opponents, effect, context,
                prompt = "Choose which opponent chooses a pile"
            )
            is ChooserResolution.Outcome.Unresolvable ->
                return EffectResult.error(state, "ChoosePile chooser: ${outcome.reason}")
        }

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // A pile choice in a per-object loop must identify the creature choosing a side.
        val choiceSubject = context.iterationEntityId ?: context.sourceId
        val choiceSubjectName = choiceSubject?.let { entityId ->
            nameVisibleToAll(state, entityId,
                state.getEntity(entityId)?.get<CardComponent>()?.name ?: "Unknown")
        }

        val cardInfo = (pileA + pileB).associateWith { cardId ->
            val cc = state.getEntity(cardId)?.get<CardComponent>()
            SearchCardInfo(
                name = cc?.name ?: "Unknown",
                manaCost = cc?.manaCost?.toString() ?: "",
                typeLine = cc?.typeLine?.toString() ?: "",
                imageUri = null,
                colors = cc?.colors?.map { it.name }?.toList() ?: emptyList()
            )
        }

        val decision = { decisionId: String -> ChooseOptionDecision(
            id = decisionId,
            playerId = deciderId,
            prompt = effect.prompt ?: "Choose a pile to keep",
            context = DecisionContext(
                sourceId = choiceSubject,
                sourceName = choiceSubjectName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = listOf(effect.pileALabel, effect.pileBLabel),
            optionCardIds = mapOf(
                0 to pileA,
                1 to pileB
            )
        ) }

        val continuation = ChoosePileContinuation(
            playerId = deciderId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            pileAIds = pileA,
            pileBIds = pileB,
            pileAName = effect.pileA,
            pileBName = effect.pileB,
            storeChosenAs = effect.storeChosenAs,
            storeOtherAs = effect.storeOtherAs,
            storedCollections = context.pipeline.storedCollections
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }
}
