package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Arcane Infusion (MID #210) — "Look at the top four cards of your library. You may reveal an
 * instant or sorcery card from among them and put it into your hand. Put the rest on the bottom of
 * your library in a random order. Flashback {3}{U}{R}"
 */
class ArcaneInfusionScenarioTest : ScenarioTestBase() {

    private fun setup(fromGraveyard: Boolean) = scenario()
        .withPlayers("Player1", "Player2")
        .let { if (fromGraveyard) it.withCardInGraveyard(1, "Arcane Infusion") else it.withCardInHand(1, "Arcane Infusion") }
        .withLandsOnBattlefield(1, "Island", 3)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Divination")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Shock") // fifth card — out of reach
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Arcane Infusion") {

            test("only instants and sorceries among the top four can be taken; the rest go to the bottom") {
                val game = setup(fromGraveyard = false)
                game.castSpell(1, "Arcane Infusion").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision() as SelectCardsDecision
                withClue("Lightning Bolt and Divination qualify; Shock is fifth and unseen") {
                    decision.options.size shouldBe 2
                }
                val divination = game.findCardsInLibrary(1, "Divination").single()
                game.selectCards(listOf(divination)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Divination") shouldBe true
                game.isInHand(1, "Lightning Bolt") shouldBe false
                withClue("the other three went to the bottom, so Shock is now on top") {
                    game.state.getLibrary(game.player1Id).first() shouldBe game.findCardsInLibrary(1, "Shock").single()
                }
            }

            test("flashback from the graveyard works and exiles the card") {
                val game = setup(fromGraveyard = true)
                game.castSpellFromGraveyard(1, "Arcane Infusion").error shouldBe null
                game.resolveStack()

                val bolt = game.findCardsInLibrary(1, "Lightning Bolt").single()
                game.selectCards(listOf(bolt)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Lightning Bolt") shouldBe true
                game.isInExile(1, "Arcane Infusion") shouldBe true
            }
        }
    }
}
