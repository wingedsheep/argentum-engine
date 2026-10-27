package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.LibrarySearchedEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import kotlin.reflect.KClass

/**
 * Tail of the [com.wingedsheep.sdk.dsl.Patterns.Library.searchLibrary] /
 * [com.wingedsheep.sdk.dsl.Patterns.Library.searchMultipleZones] /
 * [com.wingedsheep.sdk.dsl.Patterns.Library.eachPlayerSearchesLibrary] composites: emits a
 * [LibrarySearchedEvent] so "Whenever a player searches their library" triggers (CR 701.23) fire
 * exactly once per search, after the found cards have moved and the library has shuffled.
 *
 * The searching player is the one the pipeline's search gather recorded
 * ([PipelineState.SEARCHER_KEY], resolved from `GatherCardsEffect.searcher`): the destroyed land's
 * controller for Sandworm, the target player for Restorative Technique. Without a search gather
 * before it, it is [EffectContext.controllerId]. For a per-player `ForEachPlayer` search (each
 * player searches their own library) the controller is rebound to each iterated player, so the
 * event carries the correct searcher either way. Searching is the act of looking through the zone (CR 701.23a)
 * and finding a card is not required (CR 701.23b), so the event still fires when no card was found.
 */
class EmitLibrarySearchedEventExecutor : EffectExecutor<EmitLibrarySearchedEventEffect> {

    override val effectType: KClass<EmitLibrarySearchedEventEffect> = EmitLibrarySearchedEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitLibrarySearchedEventEffect,
        context: EffectContext
    ): EffectResult {
        val collections = context.pipeline.storedCollections
        val recordedSearcher = collections[PipelineState.SEARCHER_KEY]?.firstOrNull()
        val playerId = recordedSearcher ?: context.controllerId
        // A player forbidden to make this search (Shadow of Doubt, Ashiok, Dream Render) didn't
        // search, so there is no search event for "whenever a player searches their library"
        // triggers to see. The search gather decided it; without one, the search is of the
        // player's own library.
        val forbidden = if (recordedSearcher != null) {
            collections[PipelineState.SEARCH_FORBIDDEN_KEY].orEmpty().isNotEmpty()
        } else {
            LibrarySearchRestrictions.isSearchForbidden(
                state, playerId, context.effectControllerId ?: context.controllerId, listOf(playerId)
            )
        }
        if (forbidden) return EffectResult.success(state)
        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: "Search"

        return EffectResult.success(
            state,
            listOf(LibrarySearchedEvent(playerId = playerId, sourceName = sourceName))
        )
    }
}
