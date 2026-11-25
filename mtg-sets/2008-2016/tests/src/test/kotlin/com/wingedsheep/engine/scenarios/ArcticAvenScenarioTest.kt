package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Arctic Aven (M13 #42) — {2}{U} 2/1 Creature — Bird Wizard.
 *   Flying
 *   This creature gets +1/+1 as long as you control a Plains.
 *   {W}: This creature gains lifelink until end of turn.
 */
class ArcticAvenScenarioTest : ScenarioTestBase() {
    init {
        val lifelinkId = cardRegistry.getCard("Arctic Aven")!!.script.activatedAbilities.single().id

        test("without a Plains it is a plain 2/1") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Arctic Aven")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val aven = game.findPermanent("Arctic Aven")!!

            game.state.projectedState.getPower(aven) shouldBe 2
            game.state.projectedState.getToughness(aven) shouldBe 1
        }

        test("a Plains makes it 3/2, and {W} grants lifelink") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Arctic Aven")
                .withLandsOnBattlefield(1, "Plains", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val aven = game.findPermanent("Arctic Aven")!!

            game.state.projectedState.getPower(aven) shouldBe 3
            game.state.projectedState.getToughness(aven) shouldBe 2
            game.state.projectedState.hasKeyword(aven, Keyword.LIFELINK) shouldBe false

            val result = game.execute(ActivateAbility(game.player1Id, aven, lifelinkId))
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.hasKeyword(aven, Keyword.LIFELINK) shouldBe true
        }
    }
}
