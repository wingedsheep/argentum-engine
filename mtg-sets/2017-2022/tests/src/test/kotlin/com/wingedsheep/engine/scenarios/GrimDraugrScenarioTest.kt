package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Grim Draugr (KHM #96) — {1}{S}: this creature gets +1/+0 and gains menace until end of turn.
 */
class GrimDraugrScenarioTest : ScenarioTestBase() {
    private val abilityId = cardRegistry.getCard("Grim Draugr")!!.activatedAbilities.single().id

    private fun board(vararg lands: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Grim Draugr")
        .apply { lands.forEach { withLandsOnBattlefield(1, it, 1) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("{1}{S} gives +1/+0 and menace until end of turn") {
            val game = board("Snow-Covered Swamp", "Swamp")
            val draugr = game.findPermanent("Grim Draugr")!!
            game.state.projectedState.hasKeyword(draugr, Keyword.MENACE) shouldBe false

            game.execute(ActivateAbility(game.player1Id, draugr, abilityId)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(draugr) shouldBe 4
            game.state.projectedState.getToughness(draugr) shouldBe 2
            game.state.projectedState.hasKeyword(draugr, Keyword.MENACE) shouldBe true
        }

        test("two non-snow lands cannot pay {1}{S}") {
            val game = board("Swamp", "Swamp")
            game.getLegalActions(1)
                .filter { (it.action as? ActivateAbility)?.abilityId == abilityId }
                .none { it.isAffordable } shouldBe true
        }
    }
}
