package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Charmed Clothier. */
class CharmedClothierScenarioTest : ScenarioTestBase() {

    init {
        context("Charmed Clothier — Royal Role on another creature you control") {
            test("the Role lands on the other creature, not on the Clothier itself") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Charmed Clothier")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val card = game.findCardsInHand(1, "Charmed Clothier").first()
                game.execute(CastSpell(game.player1Id, card, emptyList())).error shouldBe null
                game.resolveStack() // Clothier enters -> ETB trigger asks for its target

                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                withClue("a Royal Role token exists") {
                    (game.findPermanent("Royal Role") != null) shouldBe true
                }
                withClue("2/2 Bears + Royal Role's +1/+1 = 3/3") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 3
                }

                val clothier = game.findPermanent("Charmed Clothier")!!
                withClue("the Clothier is unenchanted — 'another target creature' excludes itself") {
                    game.state.projectedState.getPower(clothier) shouldBe 3
                    game.state.projectedState.getToughness(clothier) shouldBe 3
                }
                withClue("the Clothier has flying") {
                    game.state.projectedState.hasKeyword(clothier, Keyword.FLYING) shouldBe true
                }
            }
        }
    }
}
