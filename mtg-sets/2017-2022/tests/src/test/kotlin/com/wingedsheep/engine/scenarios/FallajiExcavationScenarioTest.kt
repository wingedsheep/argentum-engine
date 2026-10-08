package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Fallaji Excavation (BRO #178) — {3}{G}{G} sorcery. "Create three tapped Powerstone tokens.
 * You gain 3 life."
 */
class FallajiExcavationScenarioTest : ScenarioTestBase() {

    init {
        test("creates three tapped Powerstone tokens and gains 3 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Fallaji Excavation")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Fallaji Excavation").error shouldBe null
            game.resolveStack()

            val powerstones = game.state.getBattlefield().filter {
                game.state.getEntity(it)?.get<CardComponent>()?.name == "Powerstone"
            }
            powerstones.size shouldBe 3
            powerstones.all { game.state.getEntity(it)!!.has<TappedComponent>() } shouldBe true
            game.getLifeTotal(1) shouldBe 23
        }
    }
}
