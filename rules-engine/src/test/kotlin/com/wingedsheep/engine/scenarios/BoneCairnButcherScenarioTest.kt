package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Bone-Cairn Butcher. */
class BoneCairnButcherScenarioTest : ScenarioTestBase() {

    private val agentRenewAbilityId =
        cardRegistry.getCard("Agent of Kotis")!!.activatedAbilities.first().id

    init {
        context("Bone-Cairn Butcher") {
            test("Mobilize 2 makes two Warrior tokens that gain deathtouch while attacking") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bone-Cairn Butcher", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Bone-Cairn Butcher" to 2))
                withClue("Declaring Bone-Cairn Butcher as attacker should succeed: ${attack.error}") {
                    attack.error shouldBe null
                }
                game.resolveStack()

                val warriors = game.findPermanents("Warrior Token")
                withClue("Mobilize 2 creates two Warrior tokens") { warriors.size shouldBe 2 }
                withClue("Each attacking Warrior token gains deathtouch from Bone-Cairn Butcher's static") {
                    warriors.forEach { token ->
                        game.state.projectedState.hasKeyword(token, Keyword.DEATHTOUCH) shouldBe true
                    }
                }
            }
        }
    }
}
