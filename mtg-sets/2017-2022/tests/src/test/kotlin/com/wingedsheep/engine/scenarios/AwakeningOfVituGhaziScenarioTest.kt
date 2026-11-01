package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Awakening of Vitu-Ghazi (WAR #152) — {3}{G}{G} instant.
 *
 *   Put nine +1/+1 counters on target land you control. It becomes a legendary 0/0 Elemental
 *   creature with haste named Vitu-Ghazi. It's still a land.
 *
 * Pins the composition: the counters plus the 0/0 base make a 9/9; the land keeps its land type
 * while gaining CREATURE, LEGENDARY, Elemental and haste; and the Layer 3 rename leaves its name as
 * only "Vitu-Ghazi" (second ruling).
 */
class AwakeningOfVituGhaziScenarioTest : ScenarioTestBase() {

    init {
        test("target land becomes a legendary 9/9 Elemental creature land with haste named Vitu-Ghazi") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Awakening of Vitu-Ghazi")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardOnBattlefield(1, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val plains = game.findPermanent("Plains")!!

            game.castSpell(1, "Awakening of Vitu-Ghazi", plains).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("nine +1/+1 counters on a 0/0 base") {
                projected.getPower(plains) shouldBe 9
                projected.getToughness(plains) shouldBe 9
            }
            withClue("it's still a land, keeping its land type") {
                projected.hasType(plains, "LAND") shouldBe true
                projected.hasSubtype(plains, "Plains") shouldBe true
            }
            withClue("a legendary Elemental creature with haste") {
                projected.isCreature(plains) shouldBe true
                projected.isLegendary(plains) shouldBe true
                projected.hasSubtype(plains, "Elemental") shouldBe true
                projected.hasKeyword(plains, Keyword.HASTE) shouldBe true
            }
            withClue("its name is only Vitu-Ghazi") {
                projected.getName(plains) shouldBe "Vitu-Ghazi"
            }
        }
    }
}
