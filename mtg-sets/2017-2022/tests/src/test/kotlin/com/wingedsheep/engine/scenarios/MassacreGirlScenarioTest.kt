package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Massacre Girl (WAR #99).
 *
 * "When Massacre Girl enters, each other creature gets -1/-1 until end of turn. Whenever a
 * creature dies this turn, each creature other than Massacre Girl gets -1/-1 until end of turn."
 *
 * What the test pins is the chain: the turn-scoped trigger must fire on the deaths it causes
 * itself, once per death, and never shrink Massacre Girl.
 *
 * Board (toughness after the enters sweep and n ripples = base - 1 - n):
 *  - Llanowar Elves 1/1 dies to the enters sweep          (ripple 1)
 *  - Grizzly Bears 2/2 dies to ripple 1                    (ripple 2)
 *  - Hill Giant 3/3 dies to ripple 2                       (ripple 3)
 *  - Craw Wurm 6/4 dies to ripple 3                        (ripple 4)
 *  - Colossal Dreadmaw 6/6 (Massacre Girl's own side) ends at 1/1 after the sweep + 4 ripples.
 */
class MassacreGirlScenarioTest : ScenarioTestBase() {

    init {
        context("Massacre Girl — the death chain") {

            test("each death ripples -1/-1 to every other creature until the chain stops") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Massacre Girl")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(1, "Colossal Dreadmaw")
                    .withCardOnBattlefield(2, "Llanowar Elves")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Craw Wurm")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Massacre Girl").error shouldBe null
                game.resolveStack()

                withClue("Every creature in the chain died") {
                    game.isInGraveyard(2, "Llanowar Elves") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.isInGraveyard(2, "Craw Wurm") shouldBe true
                }

                val dreadmaw = game.findPermanent("Colossal Dreadmaw")!!
                withClue("Dreadmaw took the sweep plus one ripple per death (-5/-5), its controller's side included") {
                    game.state.projectedState.getPower(dreadmaw) shouldBe 1
                    game.state.projectedState.getToughness(dreadmaw) shouldBe 1
                }

                val girl = game.findPermanent("Massacre Girl")!!
                withClue("Massacre Girl is excluded from the sweep and every ripple") {
                    game.state.projectedState.getPower(girl) shouldBe 4
                    game.state.projectedState.getToughness(girl) shouldBe 4
                }
            }
        }
    }
}
