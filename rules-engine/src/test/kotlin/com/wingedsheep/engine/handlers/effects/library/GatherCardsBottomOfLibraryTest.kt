package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class GatherCardsBottomOfLibraryTest : FunSpec({

    val executor = GatherCardsExecutor(predicateEvaluator = PredicateEvaluator(cardRegistry = null))

    val playerId = EntityId.generate()
    val opponentId = EntityId.generate()
    val top = EntityId.generate()
    val middle = EntityId.generate()
    val bottom = EntityId.generate()

    fun libraryState(owner: EntityId, cards: List<EntityId>): GameState {
        var state = GameState(turnOrder = listOf(playerId, opponentId))
            .withEntity(playerId, ComponentContainer())
            .withEntity(opponentId, ComponentContainer())
        for (card in cards) {
            state = state.withEntity(card, ComponentContainer())
                .addToZone(ZoneKey(owner, Zone.LIBRARY), card)
        }
        return state
    }

    fun gather(source: CardSource, state: GameState) =
        executor.execute(
            state,
            GatherCardsEffect(source = source, storeAs = "gathered"),
            EffectContext(sourceId = null, controllerId = playerId),
        )

    test("gathers the bottom card, not the top") {
        val result = gather(CardSource.BottomOfLibrary(1), libraryState(playerId, listOf(top, middle, bottom)))

        result.outcome shouldBe Outcome.Done
        result.updatedCollections["gathered"]!!.shouldContainExactly(bottom)
    }

    test("gathers the bottom N in library order") {
        val result = gather(CardSource.BottomOfLibrary(2), libraryState(playerId, listOf(top, middle, bottom)))

        result.updatedCollections["gathered"]!!.shouldContainExactly(middle, bottom)
    }

    test("reads the named player's library and caps at its size") {
        val state = libraryState(opponentId, listOf(top))

        gather(CardSource.BottomOfLibrary(3, Player.EachOpponent), state)
            .updatedCollections["gathered"]!!.shouldContainExactly(top)
        gather(CardSource.BottomOfLibrary(1, Player.You), state)
            .updatedCollections["gathered"]!!.shouldBeEmpty()
    }
})
