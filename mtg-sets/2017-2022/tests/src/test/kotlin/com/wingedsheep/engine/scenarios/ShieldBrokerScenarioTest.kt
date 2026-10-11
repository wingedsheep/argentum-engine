package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Shield Broker (NCC #29) — When this creature enters, put a shield counter on target noncommander
 * creature you don't control. You gain control of that creature for as long as it has a shield
 * counter on it.
 */
class ShieldBrokerScenarioTest : ScenarioTestBase() {

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    init {
        context("Shield Broker") {
            test("steals the creature until its shield counter is removed, then it goes back") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Shield Broker")
                    .withCardInHand(1, "Shock")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Shield Broker").error shouldBe null
                game.payIfAsked()
                game.resolveStack()

                val decision = game.getPendingDecision().shouldNotBeNull() as ChooseTargetsDecision
                withClue("only creatures you don't control: not your Bears or Shield Broker itself") {
                    decision.legalTargets[0].shouldNotBeNull() shouldContainExactlyInAnyOrder listOf(giant)
                }
                game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("Player1 controls the Hill Giant while it has a shield counter") {
                    game.state.projectedState.getController(giant) shouldBe game.player1Id
                }

                game.castSpell(1, "Shock", giant).error shouldBe null
                game.payIfAsked()
                game.resolveStack()

                withClue("Shock's damage is replaced by removing the shield counter; control reverts") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.state.projectedState.getController(giant) shouldBe game.player2Id
                }
            }
        }
    }
}
