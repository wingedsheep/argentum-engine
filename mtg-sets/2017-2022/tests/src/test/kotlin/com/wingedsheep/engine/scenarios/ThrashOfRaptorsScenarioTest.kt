package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class ThrashOfRaptorsScenarioTest : ScenarioTestBase() {
    init {
        test("itself and opposing Dinosaurs do not enable either bonus") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Thrash of Raptors")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Colossal Dreadmaw")
                .build()
            val raptors = game.findPermanent("Thrash of Raptors")!!
            game.state.projectedState.getPower(raptors) shouldBe 3
            game.state.projectedState.getToughness(raptors) shouldBe 3
            game.state.projectedState.hasKeyword(raptors, Keyword.TRAMPLE) shouldBe false
        }

        for (partner in listOf("Colossal Dreadmaw", "Universal Automaton")) {
            test("another $partner enables both bonuses until it leaves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thrash of Raptors")
                    .withCardOnBattlefield(1, partner)
                    .withCardInHand(1, "Unsummon")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val raptors = game.findPermanent("Thrash of Raptors")!!
                game.state.projectedState.getPower(raptors) shouldBe 5
                game.state.projectedState.getToughness(raptors) shouldBe 3
                game.state.projectedState.hasKeyword(raptors, Keyword.TRAMPLE) shouldBe true
                if (partner == "Universal Automaton") {
                    val automaton = game.findPermanent(partner)!!
                    game.state.projectedState.getPower(automaton) shouldBe 1
                    game.state.projectedState.hasKeyword(automaton, Keyword.TRAMPLE) shouldBe false
                }
                game.castSpell(1, "Unsummon", targetId = game.findPermanent(partner)!!).error shouldBe null
                game.resolveStack()
                game.isOnBattlefield(partner) shouldBe false
                game.state.projectedState.getPower(raptors) shouldBe 3
                game.state.projectedState.getToughness(raptors) shouldBe 3
                game.state.projectedState.hasKeyword(raptors, Keyword.TRAMPLE) shouldBe false
            }
        }
    }
}
