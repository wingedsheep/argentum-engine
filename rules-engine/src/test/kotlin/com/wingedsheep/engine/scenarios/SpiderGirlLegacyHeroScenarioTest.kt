package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Spider-Girl, Legacy Hero (SPM #149) — {G}{W} Legendary Creature —
 * Spider Human Hero, 2/2.
 *
 *   During your turn, Spider-Girl has flying.
 *   When Spider-Girl leaves the battlefield, create a 1/1 green and white Human Citizen
 *   creature token.
 *
 * Exercises the time-restricted static flying (on your turn vs. an opponent's turn — same shape
 * as Shocker, Unshakable's "during your turn, first strike") and the leaves-the-battlefield
 * trigger creating the 1/1 GW Human Citizen token.
 */
class SpiderGirlLegacyHeroScenarioTest : ScenarioTestBase() {

    init {
        context("Spider-Girl, Legacy Hero") {

            test("has flying during its controller's turn but not during an opponent's turn") {
                val onTurn = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Spider-Girl, Legacy Hero")
                    .withActivePlayer(1)
                    .build()

                val girlOnTurn = onTurn.findPermanent("Spider-Girl, Legacy Hero")!!
                withClue("2/2 with flying on your turn") {
                    onTurn.state.projectedState.getPower(girlOnTurn) shouldBe 2
                    onTurn.state.projectedState.getToughness(girlOnTurn) shouldBe 2
                    onTurn.state.projectedState.hasKeyword(girlOnTurn, Keyword.FLYING) shouldBe true
                }

                val offTurn = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Spider-Girl, Legacy Hero")
                    .withActivePlayer(2)
                    .build()

                val girlOffTurn = offTurn.findPermanent("Spider-Girl, Legacy Hero")!!
                withClue("no flying during an opponent's turn") {
                    offTurn.state.projectedState.hasKeyword(girlOffTurn, Keyword.FLYING) shouldBe false
                }
            }

            test("leaving the battlefield creates a 1/1 green and white Human Citizen token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Spider-Girl, Legacy Hero")
                    .withCardInHand(1, "Unsummon")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val girl = game.findPermanent("Spider-Girl, Legacy Hero")!!
                withClue("no Human Citizen token before Spider-Girl leaves") {
                    game.findPermanents("Human Citizen Token").size shouldBe 0
                }

                // Bounce Spider-Girl to hand — a non-death departure that still fires "leaves the battlefield".
                game.castSpell(1, "Unsummon", girl).error shouldBe null
                game.resolveStack()

                withClue("Spider-Girl left the battlefield") {
                    game.findPermanent("Spider-Girl, Legacy Hero") shouldBe null
                }

                val tokens = game.findPermanents("Human Citizen Token")
                withClue("exactly one Human Citizen token was created") {
                    tokens.size shouldBe 1
                }

                val token = tokens.single()
                withClue("token is a 1/1") {
                    game.state.projectedState.getPower(token) shouldBe 1
                    game.state.projectedState.getToughness(token) shouldBe 1
                }
                withClue("token is green and white") {
                    val colors = game.state.projectedState.getColors(token)
                    colors.contains(Color.GREEN.name) shouldBe true
                    colors.contains(Color.WHITE.name) shouldBe true
                }
            }
        }
    }
}
