package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Might of the Old Ways (MID #189) — {1}{G} Instant.
 *
 *   Target creature gets +2/+2 until end of turn.
 *   Coven — Then if you control three or more creatures with different powers, draw a card.
 *
 * Per the 2021-09-24 ruling the pump applies before the coven check, so the check must see the
 * targeted creature's boosted power.
 */
class MightOfTheOldWaysScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Might of the Old Ways")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Might of the Old Ways checks coven after the pump") {

            test("pumping a 2/2 alongside a 2/2 and a 3/3 makes powers 4, 2, 3 — draws a card") {
                val game = board()
                val bear = game.findPermanents("Grizzly Bears").first()
                val handBefore = game.handSize(1)

                game.castSpell(1, "Might of the Old Ways", bear).error shouldBe null
                game.resolveStack()

                withClue("Targeted bear is 4/4") {
                    game.state.projectedState.getPower(bear) shouldBe 4
                    game.state.projectedState.getToughness(bear) shouldBe 4
                }
                withClue("Spell left hand (-1), coven drew a card (+1)") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("pumping the 3/3 leaves powers 2, 2, 5 — no card") {
                val game = board()
                val giant = game.findPermanent("Hill Giant")!!
                val handBefore = game.handSize(1)

                game.castSpell(1, "Might of the Old Ways", giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant is 5/5") {
                    game.state.projectedState.getPower(giant) shouldBe 5
                }
                withClue("Spell left hand, no coven draw") {
                    game.handSize(1) shouldBe handBefore - 1
                }
            }
        }
    }
}
