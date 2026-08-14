package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.RIOT_MODE_OPTIONS
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Granted-Riot synthesis. Riot (CR 702.136) is composed at authoring time into an
 * [EntersWithChoice]`(MODE, [counter, haste])` + mode-gated counter / haste (see the `riot()` DSL).
 * Those printed replacement/static abilities are only read from a card's own definition, so a
 * permanent that merely has RIOT *granted* ("Other Spiders you control have riot" — Spider-Punk)
 * gets no enters-with choice. This object counts the granted instances and synthesizes that choice
 * for the entry seams (spell resolution, token/land entry); the choice resumer then applies the
 * chosen branch directly (a granted permanent has no printed `EntersWithCounters`/haste static to
 * fall back on).
 *
 * Per CR 702.136b each instance of riot works separately, so a permanent granted riot by multiple
 * lords is offered one choice per lord ([grantedRiotInstanceCount]); the resumer loops over them.
 */
object RiotSynthesis {

    /** The synthesized Riot MODE choice — identical to the one the `riot()` DSL prints. */
    val RIOT_CHOICE = EntersWithChoice(choiceType = ChoiceType.MODE, modeOptions = RIOT_MODE_OPTIONS)

    /**
     * The number of **granted** Riot instances on [entityId] — one per battlefield
     * `GrantKeyword(RIOT)` lord whose filter matches it (CR 702.136b: each instance works
     * separately). Honors each lord's `excludeSelf` (a granter never grants riot to itself) and
     * reads the lord's **projected** controller, so "Spiders you control" tracks control changes.
     *
     * Works uniformly for a spell still on the stack (its subtype/type live on its [CardComponent],
     * so the lord's filter resolves) and for a permanent already on the battlefield (token / land).
     * This scans printed `GrantKeyword(RIOT)` statics only — the sole granter shape in the card pool
     * (Spider-Punk); a granted-riot source of some other shape would not be counted here.
     */
    fun grantedRiotInstanceCount(
        state: GameState,
        entityId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): Int {
        var count = 0
        for (sourceId in state.getBattlefield()) {
            val srcCard = state.getEntity(sourceId)?.get<CardComponent>() ?: continue
            val def = cardRegistry.getCard(srcCard.cardDefinitionId) ?: continue
            val srcController = state.projectedState.getController(sourceId) ?: continue
            for (ability in def.staticAbilities) {
                if (ability !is GrantKeyword || ability.keyword != Keyword.RIOT.name) continue
                if (ability.filter.excludeSelf && sourceId == entityId) continue
                val context = PredicateContext(sourceId = sourceId, controllerId = srcController)
                if (predicateEvaluator.matches(
                        state, state.projectedState, entityId, ability.filter.baseFilter, context
                    )
                ) {
                    count++
                }
            }
        }
        return count
    }
}
