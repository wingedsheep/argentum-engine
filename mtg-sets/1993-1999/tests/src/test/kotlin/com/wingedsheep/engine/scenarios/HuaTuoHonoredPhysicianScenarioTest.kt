package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class HuaTuoHonoredPhysicianScenarioTest : ScenarioTestBase() {
    init {
        context("Hua Tuo, Honored Physician") {
            test("puts a creature card from graveyard on top of library") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Hua Tuo, Honored Physician", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val src = game.findPermanent("Hua Tuo, Honored Physician")!!
                val bears = game.state.getGraveyard(game.player1Id).first()
                val abilityId = cardRegistry.getCard("Hua Tuo, Honored Physician")!!.script.activatedAbilities[0].id
                game.execute(
                    ActivateAbility(game.player1Id, src, abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, bears)))
                ).error shouldBe null
                game.resolveStack()
                game.graveyardSize(1) shouldBe 0
                game.state.getLibrary(game.player1Id).first() shouldBe bears
            }

            test("cannot be activated after attackers are declared") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Hua Tuo, Honored Physician", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val src = game.findPermanent("Hua Tuo, Honored Physician")!!
                val bears = game.state.getGraveyard(game.player1Id).first()
                val abilityId = cardRegistry.getCard("Hua Tuo, Honored Physician")!!.script.activatedAbilities[0].id
                game.execute(
                    ActivateAbility(game.player1Id, src, abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, bears)))
                ).error shouldNotBe null
            }
        }
    }
}
