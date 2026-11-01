package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Tenth District Veteran (RNA #26) — "Whenever this creature attacks, untap another target creature
 * you control."
 */
class TenthDistrictVeteranScenarioTest : ScenarioTestBase() {
    init {
        test("attacking untaps a fellow attacker") {
            val game = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Tenth District Veteran")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Tenth District Veteran" to 2, "Grizzly Bears" to 2)).error shouldBe null
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true

            game.getPendingDecision().shouldNotBeNull()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
        }
    }
}
