package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Buried Alive — {2}{B} Sorcery.
 * "Search your library for up to three creature cards, put them into your graveyard, then shuffle."
 */
class BuriedAliveScenarioTest : ScenarioTestBase() {

    private fun setup() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Buried Alive")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Whiptail Wurm")
        .withCardInLibrary(1, "Gravedigger")
        .withCardInLibrary(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("offers only creature cards, up to three, and puts the chosen ones into the graveyard") {
            val game = setup()
            game.castSpell(1, "Buried Alive").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.minSelections shouldBe 0
            decision.maxSelections shouldBe 3
            val info = decision.cardInfo!!
            decision.options.map { info[it]!!.name } shouldContainExactlyInAnyOrder
                listOf("Grizzly Bears", "Hill Giant", "Whiptail Wurm", "Gravedigger")

            val picks = decision.options.filter { info[it]!!.name in setOf("Grizzly Bears", "Hill Giant", "Gravedigger") }
            game.selectCards(picks)

            withClue("chosen creatures are in the graveyard") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                game.isInGraveyard(1, "Gravedigger") shouldBe true
            }
            game.isInGraveyard(1, "Whiptail Wurm") shouldBe false
            game.isInGraveyard(1, "Buried Alive") shouldBe true
            game.librarySize(1) shouldBe 2
            game.graveyardSize(1) shouldBe 4
        }

        test("may find zero creatures") {
            val game = setup()
            game.castSpell(1, "Buried Alive").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(emptyList())

            game.librarySize(1) shouldBe 5
            game.graveyardSize(1) shouldBe 1
            game.isInGraveyard(1, "Buried Alive") shouldBe true
        }
    }
}
