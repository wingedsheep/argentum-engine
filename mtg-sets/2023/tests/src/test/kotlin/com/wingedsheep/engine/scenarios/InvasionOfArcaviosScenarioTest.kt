package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Invasion of Arcavios // Invocation of the Founders.
 *
 * Front: the enter trigger searches library, graveyard, and outside the game for an instant or
 * sorcery and puts it into hand.
 */
class InvasionOfArcaviosScenarioTest : ScenarioTestBase() {

    init {
        test("enter trigger fetches an instant or sorcery from the sideboard, ignoring creatures") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Arcavios")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInSideboard(1, "Swelter")
                .withCardInSideboard(1, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Arcavios").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as? SelectCardsDecision
            decision.shouldNotBeNull()
            val offered = decision.cardInfo!!
            withClue("only the sorcery is offered") {
                offered.values.any { it.name == "Swelter" } shouldBe true
                offered.values.any { it.name == "Grizzly Bears" } shouldBe false
            }
            game.selectCards(listOf(offered.entries.first { it.value.name == "Swelter" }.key))

            game.isInHand(1, "Swelter") shouldBe true
            game.isInSideboard(1, "Swelter") shouldBe false
        }

        test("enter trigger fetches an instant from the graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Invasion of Arcavios")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInGraveyard(1, "Glorious Gale")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Invasion of Arcavios").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as? SelectCardsDecision
            decision.shouldNotBeNull()
            game.selectCards(listOf(decision.cardInfo!!.entries.first { it.value.name == "Glorious Gale" }.key))

            game.isInHand(1, "Glorious Gale") shouldBe true
        }
    }
}
