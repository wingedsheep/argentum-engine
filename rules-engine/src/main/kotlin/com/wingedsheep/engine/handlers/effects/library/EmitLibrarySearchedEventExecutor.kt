package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.LibrarySearchedEvent
import com.wingedsheep.engine.handlers.EffectContext
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
 * The searching player is [EffectContext.controllerId]. For a per-player `ForEachPlayer` search
 * (each player searches their own library) that id is rebound to each iterated player, so the
 * event carries the correct searcher. Searching is the act of looking through the zone (CR 701.23a)
 * and finding a card is not required (CR 701.23b), so the event still fires when no card was found.
 */
class EmitLibrarySearchedEventExecutor : EffectExecutor<EmitLibrarySearchedEventEffect> {

    override val effectType: KClass<EmitLibrarySearchedEventEffect> = EmitLibrarySearchedEventEffect::class

    override fun execute(
        state: GameState,
        effect: EmitLibrarySearchedEventEffect,
        context: EffectContext
    ): EffectResult {
        val playerId = context.controllerId
        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }
            ?: "Search"

        return EffectResult.success(
            state,
            listOf(LibrarySearchedEvent(playerId = playerId, sourceName = sourceName))
        )
    }
}
