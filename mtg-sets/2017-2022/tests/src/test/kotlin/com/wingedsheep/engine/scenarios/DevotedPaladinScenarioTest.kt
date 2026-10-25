package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Devoted Paladin (AFR #11): Beacon of Hope — entering gives each creature you control (itself
 * included) +1/+1 and vigilance until end of turn; opponents' creatures are untouched.
 */
class DevotedPaladinScenarioTest : ScenarioTestBase() {

    init {
        test("entering pumps and grants vigilance to creatures you control only") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Devoted Paladin")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Devoted Paladin").error shouldBe null
            game.resolveStack()

            val paladin = game.findPermanent("Devoted Paladin")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val projected = game.state.projectedState

            withClue("Devoted Paladin pumps itself") {
                projected.getPower(paladin) shouldBe 5
                projected.getToughness(paladin) shouldBe 5
                projected.hasKeyword(paladin, Keyword.VIGILANCE) shouldBe true
            }
            withClue("Another creature you control is pumped") {
                projected.getPower(bears) shouldBe 3
                projected.getToughness(bears) shouldBe 3
                projected.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
            }
            withClue("The opponent's creature is untouched") {
                projected.getPower(giant) shouldBe 3
                projected.getToughness(giant) shouldBe 3
                projected.hasKeyword(giant, Keyword.VIGILANCE) shouldBe false
            }
        }
    }
}
