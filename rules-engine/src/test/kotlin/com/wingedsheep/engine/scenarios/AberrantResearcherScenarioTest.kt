package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Aberrant Researcher // Perfected Form (SOI #49).
 *
 * "Flying
 *  At the beginning of your upkeep, mill a card. If an instant or sorcery card was milled this way,
 *  transform this creature."
 *
 * The mill is mandatory either way; only the transform is gated. Both branches are covered, plus the
 * printed ruling that no player acts between the mill and the transform (they resolve as one ability,
 * so the flip is already done when the stack is next empty).
 */
class AberrantResearcherScenarioTest : ScenarioTestBase() {

    init {
        context("Aberrant Researcher") {

            /** Player 1 controls the Researcher; [topCard] is the top of their library. */
            fun gameWithTopCard(topCard: String) = run {
                var builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Aberrant Researcher", summoningSickness = false)
                    .withCardInLibrary(1, topCard) // first added = index 0 = top
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Grizzly Bears") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Grizzly Bears") }
                builder.build()
            }

            test("milling an instant transforms it into Perfected Form") {
                val game = gameWithTopCard("Lightning Bolt")
                val researcher = game.findPermanent("Aberrant Researcher")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("the card was milled") {
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                }
                withClue("an instant was milled → transformed, with no window in between") {
                    game.state.getEntity(researcher)!!.get<CardComponent>()!!.name shouldBe "Perfected Form"
                }
            }

            test("milling a creature card does not transform it") {
                val game = gameWithTopCard("Grizzly Bears")
                val researcher = game.findPermanent("Aberrant Researcher")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("the mill still happens") {
                    game.graveyardSize(1) shouldBe 1
                }
                withClue("no instant or sorcery milled → stays Aberrant Researcher") {
                    game.state.getEntity(researcher)!!.get<CardComponent>()!!.name shouldBe "Aberrant Researcher"
                }
            }
        }
    }
}
