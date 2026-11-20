package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Beetleform Mage (DGM #54) — {1}{G}{U} 2/2 Creature — Human Insect Wizard.
 *
 *   {G}{U}: This creature gets +2/+2 and gains flying until end of turn. Activate only once each turn.
 */
class BeetleformMageScenarioTest : ScenarioTestBase() {
    init {
        val pumpId = cardRegistry.getCard("Beetleform Mage")!!.script.activatedAbilities.single().id

        test("pumps +2/+2 with flying, only once per turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Beetleform Mage")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mage = game.findPermanent("Beetleform Mage")!!

            game.state.projectedState.hasKeyword(mage, Keyword.FLYING) shouldBe false

            val result = game.execute(ActivateAbility(game.player1Id, mage, pumpId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(mage) shouldBe 4
            projected.getToughness(mage) shouldBe 4
            projected.hasKeyword(mage, Keyword.FLYING) shouldBe true

            withClue("a second activation this turn is refused even with mana open") {
                game.execute(ActivateAbility(game.player1Id, mage, pumpId)).error shouldNotBe null
            }
        }
    }
}
