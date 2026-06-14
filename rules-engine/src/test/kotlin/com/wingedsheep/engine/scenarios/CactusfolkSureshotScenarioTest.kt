package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Cactusfolk Sureshot (OTJ #199).
 *
 * {2}{R}{G} · Creature — Plant Mercenary · 4/4
 * Reach, Ward {2}
 * "At the beginning of combat on your turn, other creatures you control with power 4 or
 * greater gain trample and haste until end of turn."
 *
 * Verifies the begin-combat trigger: every *other* creature you control whose power is ≥ 4
 * at resolution gains both trample and haste; a power-3-or-less creature is skipped, and
 * Cactusfolk itself (the source) is excluded by the "other" clause.
 */
class CactusfolkSureshotScenarioTest : FunSpec({

    val projector = StateProjector()

    fun GameTestDriver.advanceToPlayer1BeginCombat() {
        passPriorityUntil(Step.BEGIN_COMBAT)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.BEGIN_COMBAT)
            safety++
        }
    }

    test("begin-combat trigger grants trample and haste to other power-4+ creatures only") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)

        val cactusfolk = driver.putCreatureOnBattlefield(driver.player1, "Cactusfolk Sureshot")
        val crawWurm = driver.putCreatureOnBattlefield(driver.player1, "Craw Wurm") // 6/4, power >= 4
        val bears = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") // 2/2, power < 4
        val theirWurm = driver.putCreatureOnBattlefield(
            driver.getOpponent(driver.player1),
            "Craw Wurm"
        ) // opponent's power-6 creature is untouched

        driver.advanceToPlayer1BeginCombat()
        // Resolve the begin-combat trigger.
        driver.bothPass()

        // Power-6 creature I control gains both keywords.
        projector.hasProjectedKeyword(driver.state, crawWurm, Keyword.TRAMPLE) shouldBe true
        projector.hasProjectedKeyword(driver.state, crawWurm, Keyword.HASTE) shouldBe true

        // Power-2 creature is below the threshold — unaffected.
        projector.hasProjectedKeyword(driver.state, bears, Keyword.TRAMPLE) shouldBe false
        projector.hasProjectedKeyword(driver.state, bears, Keyword.HASTE) shouldBe false

        // Cactusfolk itself is excluded by "other creatures".
        projector.hasProjectedKeyword(driver.state, cactusfolk, Keyword.TRAMPLE) shouldBe false
        projector.hasProjectedKeyword(driver.state, cactusfolk, Keyword.HASTE) shouldBe false

        // Opponent's creature is untouched.
        projector.hasProjectedKeyword(driver.state, theirWurm, Keyword.TRAMPLE) shouldBe false
        projector.hasProjectedKeyword(driver.state, theirWurm, Keyword.HASTE) shouldBe false
    }
})
