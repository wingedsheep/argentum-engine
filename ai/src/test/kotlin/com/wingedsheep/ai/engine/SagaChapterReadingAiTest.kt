package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Saga chapter abilities (CR 714.2b) are triggered abilities the AI's readers never saw: every
 * Saga read as "nothing to read" and sat on the board at the flat 0.5 an unreadable permanent
 * gets, whatever its chapters still had in store.
 */
class SagaChapterReadingAiTest : ScenarioTestBase() {

    init {
        fun sacrificeChoice(lore: Int): List<String> {
            val game = scenario()
                .withPlayers()
                .withTurnNumber(8)
                .withActivePlayer(2)
                .withLandsOnBattlefield(2, "Forest", 1)
                .withCardInHand(2, "Pick Your Poison")
                .withCardOnBattlefield(1, "The Wrath of Khan")
                .withCardOnBattlefield(1, "Raging River")
                // An inert filler (see PuzzleRunner.stockLibraries): an empty library decks a rollout.
                .apply { repeat(20) { withCardInLibrary(1, "Craw Wurm"); withCardInLibrary(2, "Craw Wurm") } }
                .build()
            val saga = game.findPermanent("The Wrath of Khan")!!
            game.state = game.state.updateEntity(saga) { c ->
                c.with(CountersComponent().withCounters(CounterType.LORE, lore))
                    .with(SagaComponent(triggeredChapters = (1..lore).toSet()))
            }

            // "Each opponent sacrifices an enchantment of their choice."
            game.castSpellWithMode(2, "Pick Your Poison", modeIndex = 1).error shouldBe null
            game.resolveStack()
            val decision = game.state.pendingDecision.shouldNotBeNull()

            val response = AIPlayer.create(cardRegistry, game.player1Id, AiProfile.LIVE).respondToDecision(game.state, decision)
            return response.shouldBeInstanceOf<CardsSelectedResponse>().selectedCards.map {
                game.state.getEntity(it)!!.get<CardComponent>()!!.name
            }
        }

        test("keeps the Saga whose chapters are still to come when made to sacrifice an enchantment") {
            withClue("a Saga reads as the chapters it has left: chapter II's loot and chapter III's edict") {
                sacrificeChoice(lore = 1) shouldBe listOf("Raging River")
            }
        }
    }
}
