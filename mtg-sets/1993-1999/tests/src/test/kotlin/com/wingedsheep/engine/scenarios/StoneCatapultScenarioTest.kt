package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class StoneCatapultScenarioTest : ScenarioTestBase() {
    private fun activate(game: TestGame, targetName: String) = run {
        val src = game.findPermanent("Stone Catapult")!!
        val abilityId = cardRegistry.getCard("Stone Catapult")!!.script.activatedAbilities[0].id
        game.execute(
            ActivateAbility(game.player1Id, src, abilityId,
                targets = listOf(entityIdToChosenTarget(game.state, game.findPermanent(targetName)!!)))
        )
    }

    init {
        context("Stone Catapult") {
            test("destroys a tapped nonblack creature") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Stone Catapult", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                activate(game, "Hill Giant").error shouldBe null
                game.resolveStack()
                game.isOnBattlefield("Hill Giant") shouldBe false
            }

            test("cannot target an untapped creature") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Stone Catapult", summoningSickness = false)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                (activate(game, "Hill Giant").error != null) shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }

            test("cannot target a black creature") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Stone Catapult", summoningSickness = false)
                    .withCardOnBattlefield(2, "Wei Assassins", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                (activate(game, "Wei Assassins").error != null) shouldBe true
                game.isOnBattlefield("Wei Assassins") shouldBe true
            }
        }
    }
}
