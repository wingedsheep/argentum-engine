package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Darkthicket Wolf (ISD #175) — {1}{G} 2/2 Wolf.
 *
 *   {2}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn.
 */
class DarkthicketWolfScenarioTest : ScenarioTestBase() {

    init {
        val pumpId = cardRegistry.getCard("Darkthicket Wolf")!!.script.activatedAbilities.single().id

        test("pumps +2/+2, but only once each turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Darkthicket Wolf")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val wolf = game.findPermanent("Darkthicket Wolf")!!
            val first = game.execute(ActivateAbility(game.player1Id, wolf, pumpId))
            withClue("first activation should succeed: ${first.error}") { first.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(wolf) shouldBe 4
            game.state.projectedState.getToughness(wolf) shouldBe 4

            withClue("a second activation in the same turn is refused even with mana available") {
                game.execute(ActivateAbility(game.player1Id, wolf, pumpId)).error shouldNotBe null
            }
            game.state.projectedState.getPower(wolf) shouldBe 4
        }
    }
}
