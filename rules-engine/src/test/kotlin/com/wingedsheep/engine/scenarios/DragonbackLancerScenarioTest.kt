package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Dragonback Lancer. */
class DragonbackLancerScenarioTest : ScenarioTestBase() {

    private val agentRenewAbilityId =
        cardRegistry.getCard("Agent of Kotis")!!.activatedAbilities.first().id

    init {
        context("Dragonback Lancer") {
            test("flying, and Mobilize 1 makes one tapped, attacking Warrior token on attack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Dragonback Lancer", tapped = false, summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lancer = game.findPermanent("Dragonback Lancer")!!
                withClue("Dragonback Lancer has flying") {
                    game.state.projectedState.hasKeyword(lancer, Keyword.FLYING) shouldBe true
                }

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val attack = game.declareAttackers(mapOf("Dragonback Lancer" to 2))
                withClue("Declaring Dragonback Lancer as attacker should succeed: ${attack.error}") {
                    attack.error shouldBe null
                }
                game.resolveStack()

                val warriors = game.findPermanents("Warrior Token")
                withClue("Mobilize 1 creates one Warrior token") { warriors.size shouldBe 1 }
                withClue("The Warrior token is tapped and attacking") {
                    warriors.forEach { token ->
                        game.state.getEntity(token)?.has<TappedComponent>() shouldBe true
                        game.state.getEntity(token)?.has<AttackingComponent>() shouldBe true
                    }
                }
            }
        }
    }
}
