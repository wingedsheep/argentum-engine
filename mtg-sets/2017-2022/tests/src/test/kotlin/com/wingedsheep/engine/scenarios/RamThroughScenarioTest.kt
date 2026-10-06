package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ram Through (IKO #170) — {1}{G} Instant.
 *
 *   Target creature you control deals damage equal to its power to target creature you don't
 *   control. If the creature you control has trample, excess damage is dealt to that creature's
 *   controller instead.
 *
 * The bite is sourced from the creature you control, and the excess-to-controller routing is
 * gated on that creature having trample as the spell resolves.
 */
class RamThroughScenarioTest : ScenarioTestBase() {

    private fun cast(game: TestGame, mine: String, theirs: String) {
        val spell = game.findCardsInHand(1, "Ram Through").first()
        game.execute(
            CastSpell(
                game.player1Id,
                spell,
                listOf(
                    ChosenTarget.Permanent(game.findPermanent(mine)!!),
                    ChosenTarget.Permanent(game.findPermanent(theirs)!!)
                )
            )
        ).error shouldBe null
        game.resolveStack()
    }

    private fun board(mine: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Ram Through")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardOnBattlefield(1, mine)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Ram Through") {

            test("a trampler deals the excess past lethal to the bitten creature's controller") {
                val game = board("Colossal Dreadmaw") // 6/6 trample
                cast(game, "Colossal Dreadmaw", "Grizzly Bears")

                withClue("the 2/2 takes lethal and dies") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("6 power minus 2 lethal = 4 excess to its controller") {
                    game.getLifeTotal(2) shouldBe 16
                }
                game.getLifeTotal(1) shouldBe 20
            }

            test("without trample all the damage goes to the creature") {
                val game = board("Craw Wurm") // 6/4, no trample
                cast(game, "Craw Wurm", "Grizzly Bears")

                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                withClue("no excess is routed to the controller without trample") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }
        }
    }
}
