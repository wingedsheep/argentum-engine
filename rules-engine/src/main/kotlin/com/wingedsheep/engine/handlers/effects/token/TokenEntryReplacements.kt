package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.RiotSynthesis
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersWithChoice

/**
 * The "as-enters" replacement pipeline shared by every path that puts a **token** onto the
 * battlefield — [TokenFromDefinition.mint] (a token minted from a bare card definition) and the four
 * token-copy executors ([CreateTokenCopyOfTargetExecutor], [CreateTokenCopyOfSourceExecutor],
 * [CreateTokenCopyOfChosenPermanentExecutor], [CreateTokenCopyOfEquippedCreatureExecutor]).
 *
 * Those executors place their tokens via the ad-hoc [com.wingedsheep.engine.handlers.effects.BattlefieldEntry.place]
 * path, which deliberately skips all enters-with replacement setup. Per CR 707.2 a copy has the
 * copied card's abilities, so a token copy must still run the copied card's "enters with counters"
 * (CR 614.1c) and "as this enters, choose …" (CR 614.12) replacements, plus any **granted** riot
 * (CR 702.136 / 702.136b — each granted instance is a separate choice) from a lord such as
 * Spider-Punk.
 *
 * Counters are applied inline by the caller through
 * [com.wingedsheep.engine.handlers.effects.EntersWithReplacements.applyOnEntry] (non-pausing). The
 * choice half is what needs a player decision mid-loop, so this object centralizes computing **which**
 * choice a just-placed token owes; the caller then pauses via
 * [com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements.pauseForEntersWithChoice],
 * whose existing [com.wingedsheep.engine.core.EntersWithChoiceOnBattlefieldContinuation] resumer
 * records the value, applies the granted-riot branch, loops per riot instance, chains any further
 * printed choice, and fires the token's ETB triggers.
 */
object TokenEntryReplacements {

    private val predicateEvaluator = PredicateEvaluator()

    /**
     * The [EntersWithChoice] a just-placed token owes as it enters, together with the granted-riot
     * bookkeeping the choice pause needs. `null` when the token owes no choice (the common case).
     *
     * @property choice the first choice to present (printed choices and a synthesized granted-riot
     *   choice are pooled and ordered by [com.wingedsheep.sdk.scripting.ChoiceType.ordinal], matching
     *   [TokenFromDefinition]).
     * @property syntheticRiot true when [choice] is the synthesized granted-riot choice (the resumer
     *   then applies the chosen +1/+1-counter / haste branch directly — a granted permanent has no
     *   printed static to fall back on).
     * @property syntheticRiotRemaining the number of *further* granted-riot choices still owed after
     *   [choice] (CR 702.136b), 0 unless [syntheticRiot].
     */
    data class EntersWithChoicePlan(
        val choice: EntersWithChoice,
        val syntheticRiot: Boolean,
        val syntheticRiotRemaining: Int,
    )

    /**
     * Compute the first "as-enters" choice a token already on the battlefield owes, pooling the copied
     * card's printed [EntersWithChoice]s (looked up by the token's `cardDefinitionId`) with a
     * synthesized granted-riot choice. Returns `null` when there is nothing to choose.
     *
     * Kept identical in ordering to [TokenFromDefinition] so a minted token and a token copy resolve
     * their as-enters choices the same way.
     */
    fun firstEntersWithChoice(
        state: GameState,
        tokenId: EntityId,
        cardRegistry: CardRegistry,
    ): EntersWithChoicePlan? {
        val cardComponent = state.getEntity(tokenId)?.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        val printedChoices = cardDef?.script?.replacementEffects
            ?.filterIsInstance<EntersWithChoice>()
            ?: emptyList()
        val grantedRiotCount = RiotSynthesis.grantedRiotInstanceCount(
            state, tokenId, cardRegistry, predicateEvaluator
        )
        val syntheticRiotChoice = if (grantedRiotCount > 0) RiotSynthesis.RIOT_CHOICE else null
        val firstChoice = (printedChoices + listOfNotNull(syntheticRiotChoice))
            .sortedBy { it.choiceType.ordinal }
            .firstOrNull() ?: return null
        val isSynthetic = firstChoice === syntheticRiotChoice
        return EntersWithChoicePlan(
            choice = firstChoice,
            syntheticRiot = isSynthetic,
            syntheticRiotRemaining = if (isSynthetic) grantedRiotCount - 1 else 0,
        )
    }
}
