package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Hailstorm Valkyrie (KHM #97) — flying, trample; {S}{S}: this creature gets +2/+2 until end of turn.
 */
class HailstormValkyrieScenarioTest : ScenarioTestBase() {
    private val abilityId = cardRegistry.getCard("Hailstorm Valkyrie")!!.activatedAbilities.single().id

    private fun board(vararg lands: String) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Hailstorm Valkyrie")
        .apply { lands.forEach { withLandsOnBattlefield(1, it, 1) } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("two snow mana pump it +2/+2; it keeps flying and trample") {
            val game = board("Snow-Covered Swamp", "Snow-Covered Swamp")
            val valkyrie = game.findPermanent("Hailstorm Valkyrie")!!

            game.execute(ActivateAbility(game.player1Id, valkyrie, abilityId)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(valkyrie) shouldBe 4
            game.state.projectedState.getToughness(valkyrie) shouldBe 4
            game.state.projectedState.hasKeyword(valkyrie, Keyword.FLYING) shouldBe true
            game.state.projectedState.hasKeyword(valkyrie, Keyword.TRAMPLE) shouldBe true
        }

        test("one snow and one non-snow land cannot pay {S}{S}") {
            val game = board("Snow-Covered Swamp", "Swamp")
            game.getLegalActions(1)
                .filter { (it.action as? ActivateAbility)?.abilityId == abilityId }
                .none { it.isAffordable } shouldBe true
        }
    }
}
