package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.ChooserResolution
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChoosePileEffect
import kotlin.reflect.KClass

/**
 * Executor for [ChoosePileEffect].
 *
 * Reads two or more named card collections from the pipeline ([ChoosePileEffect.piles]), presents
 * a [ChooseOptionDecision] (one option per pile) to the player [ChooserResolution] derives from the
 * effect's [com.wingedsheep.sdk.scripting.effects.Chooser], and (on response) routes the picked
 * pile to [ChoosePileEffect.storeChosenAs] and every other pile, merged, to
 * [ChoosePileEffect.storeOtherAs]. With [ChoosePileEffect.atRandom] the engine draws the pile
 * uniformly with the seeded RNG and nothing is presented.
 */
class ChoosePileExecutor : EffectExecutor<ChoosePileEffect> {

    override val effectType: KClass<ChoosePileEffect> = ChoosePileEffect::class

    override fun execute(
        state: GameState,
        effect: ChoosePileEffect,
        context: EffectContext
    ): EffectResult {
        val piles = effect.piles.map { context.pipeline.storedCollections[it] ?: emptyList() }

        if (effect.atRandom) {
            // "Chooses one of their piles at random" — each pile, empty or not, has an equal
            // chance; no player makes this choice, so nothing is presented.
            val (pick, advanced) = state.nextRandom { nextInt(piles.size) }
            return EffectResult.success(advanced).copy(
                updatedCollections = ChoosePileRouting.route(piles, pick, effect.storeChosenAs, effect.storeOtherAs)
            )
        }

        val allCards = piles.flatten()
        val deciderId = when (
            val outcome = ChooserResolution.resolve(state, effect.chooser, context, allCards)
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

        val cardInfo = allCards.associateWith { cardId ->
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
            options = effect.pileLabels,
            optionCardIds = piles.withIndex().associate { (index, pile) -> index to pile }
        ) }

        val continuation = ChoosePileContinuation(
            playerId = deciderId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            pileIds = piles,
            storeChosenAs = effect.storeChosenAs,
            storeOtherAs = effect.storeOtherAs,
            storedCollections = context.pipeline.storedCollections
        )

        return EffectResult.from(state.suspendForDecision(decision, continuation))
    }
}

/** Stores the picked pile under one name and every other pile, merged, under another. */
internal object ChoosePileRouting {
    fun route(
        piles: List<List<EntityId>>,
        chosenIndex: Int,
        storeChosenAs: String,
        storeOtherAs: String
    ): Map<String, List<EntityId>> = mapOf(
        storeChosenAs to piles[chosenIndex],
        storeOtherAs to piles.filterIndexed { index, _ -> index != chosenIndex }.flatten()
    )
}
