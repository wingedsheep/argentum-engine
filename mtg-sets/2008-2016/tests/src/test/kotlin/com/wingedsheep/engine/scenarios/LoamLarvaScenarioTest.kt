package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Loam Larva — {1}{G} 1/3 Insect. "When this creature enters, you may search your library for a
 * basic land card, reveal it, then shuffle and put that card on top."
 */
class LoamLarvaScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Loam Larva")
            .withLandsOnBattlefield(1, "Forest", 2)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        test("enters: search for a basic land and put it on top of the library") {
            val game = board()
            val swamp = game.findCardsInLibrary(1, "Swamp").single()

            game.castSpell(1, "Loam Larva").error shouldBe null
            game.resolveStack()

            withClue("the search is optional") { game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>() }
            game.answerYesNo(true).error shouldBe null

            val decision = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only basic lands are offered") {
                decision.options shouldContain swamp
                decision.options.size shouldBe 1
            }
            game.selectCards(listOf(swamp)).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Loam Larva") shouldBe true
            withClue("the found land is on top of the library, not in hand") {
                game.state.getLibrary(game.player1Id).first() shouldBe swamp
                game.state.getHand(game.player1Id) shouldNotContain swamp
            }
            game.librarySize(1) shouldBe 3
        }

        test("declining the search leaves the library alone") {
            val game = board()
            val libraryBefore = game.state.getLibrary(game.player1Id)

            game.castSpell(1, "Loam Larva").error shouldBe null
            game.resolveStack()
            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.state.getLibrary(game.player1Id) shouldBe libraryBefore
        }
    }
}
