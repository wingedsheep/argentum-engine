package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice

/**
 * Granted-read-ahead synthesis. Read ahead (CR 702.155) is composed at authoring time into an
 * [EntersWithChoice]`(NUMBER, 1..final chapter)` plus the [Keyword.READ_AHEAD] tag (see the
 * `readAhead()` DSL). That printed choice is only read from a card's own definition, so a Saga that
 * merely has read ahead *granted* ("Sagas you control have read ahead" — Barbara Wright) would get
 * no choice. This object synthesizes the same choice for the entry seams (spell resolution, land
 * play, token entry, an effect's put-onto-the-battlefield), the way [RiotSynthesis] does for riot.
 *
 * The answer lands in the ordinary `CHOSEN_NUMBER` slot, which
 * [com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.sagaEntryLoreCount] reads, and the
 * chapter-skip rule reads READ_AHEAD off projected state, where the grant shows up once the Saga is
 * on the battlefield. Read ahead is not cumulative — a Saga enters with one chosen number however
 * many sources grant it — so a single choice is synthesized, never one per granter.
 */
object ReadAheadSynthesis {

    /**
     * The synthesized read-ahead choice for [entityId] entering as [def], or null when [def] is not
     * a Saga, already has read ahead printed (its own choice covers it), or no battlefield
     * `GrantKeyword(READ_AHEAD)` matches it. The granter's filter is matched against [entityId]'s
     * current controller, so a caller entering it under someone else sets that controller first.
     */
    fun grantedChoice(
        state: GameState,
        entityId: EntityId,
        def: CardDefinition?,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): EntersWithChoice? {
        if (def == null || def.sagaChapters.isEmpty() || Keyword.READ_AHEAD in def.keywords) return null
        if (RiotSynthesis.grantedKeywordInstanceCount(
                state, entityId, Keyword.READ_AHEAD, cardRegistry, predicateEvaluator
            ) == 0
        ) return null
        return EntersWithChoice(
            choiceType = ChoiceType.NUMBER,
            minValue = 1,
            maxValue = def.sagaChapters.maxOf { it.chapter },
        )
    }
}
