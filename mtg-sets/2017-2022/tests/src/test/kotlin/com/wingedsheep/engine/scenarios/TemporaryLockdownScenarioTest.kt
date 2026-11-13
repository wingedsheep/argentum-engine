package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Temporary Lockdown (DMU #36) — {1}{W}{W} Enchantment.
 *
 *   When this enchantment enters, exile each nonland permanent with mana value 2 or less until
 *   this enchantment leaves the battlefield.
 */
class TemporaryLockdownScenarioTest : ScenarioTestBase() {

    private fun TestGame.payIfAsked() {
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
    }

    private fun lockdownBoard() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Savannah Lions")                 // MV 1, your own
        .withCardInHand(1, "Temporary Lockdown")
        .withCardInHand(1, "Disenchant")
        .withLandsOnBattlefield(1, "Plains", 5)
        .withCardOnBattlefield(2, "Grizzly Bears")                  // MV 2
        .withCardOnBattlefield(2, "Bonesplitter")                   // MV 1 artifact
        .withCardOnBattlefield(2, "Hill Giant")                     // MV 4, stays
        .withCardOnBattlefield(2, "Elite Vanguard", isToken = true) // token, ceases to exist
        .withLandsOnBattlefield(2, "Forest", 1)                     // land, stays
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Temporary Lockdown") {

            test("exiles every nonland permanent with mana value 2 or less until it leaves") {
                val game = lockdownBoard()

                game.castSpell(1, "Temporary Lockdown").error shouldBe null
                game.payIfAsked()
                game.resolveStack() // the enchantment resolves, then its enters trigger

                withClue("each nonland permanent with mana value 2 or less is exiled, both sides") {
                    game.isOnBattlefield("Savannah Lions") shouldBe false
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Bonesplitter") shouldBe false
                    game.isOnBattlefield("Elite Vanguard") shouldBe false
                    game.isInExile(1, "Savannah Lions") shouldBe true
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInExile(2, "Bonesplitter") shouldBe true
                }
                withClue("mana value 3+ permanents, lands, and the enchantment itself stay") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.isOnBattlefield("Forest") shouldBe true
                    game.isOnBattlefield("Temporary Lockdown") shouldBe true
                }

                val lockdown = game.findPermanent("Temporary Lockdown")!!
                game.castSpell(1, "Disenchant", lockdown).error shouldBe null
                game.payIfAsked()
                game.resolveStack() // Disenchant; the cards return the moment the enchantment leaves

                game.isInGraveyard(1, "Temporary Lockdown") shouldBe true
                withClue("the exiled cards return under their owners' control") {
                    val lions = game.findPermanent("Savannah Lions").shouldNotBeNull()
                    val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                    val bonesplitter = game.findPermanent("Bonesplitter").shouldNotBeNull()
                    game.state.projectedState.getController(lions) shouldBe game.player1Id
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                    game.state.projectedState.getController(bonesplitter) shouldBe game.player2Id
                }
                withClue("the exiled token ceased to exist and does not come back") {
                    game.isOnBattlefield("Elite Vanguard") shouldBe false
                }
            }

            test("if it leaves before its enters trigger resolves, nothing is exiled") {
                val game = lockdownBoard()

                game.castSpell(1, "Temporary Lockdown").error shouldBe null
                game.payIfAsked()
                // Both pass once: the enchantment resolves and its enters trigger goes on the stack.
                game.passPriority().error shouldBe null
                game.passPriority().error shouldBe null
                game.isOnBattlefield("Temporary Lockdown") shouldBe true
                game.state.stack.size shouldBe 1

                // Destroy it in response to its own enters trigger.
                val lockdown = game.findPermanent("Temporary Lockdown")!!
                game.castSpell(1, "Disenchant", lockdown).error shouldBe null
                game.payIfAsked()
                game.resolveStack()

                game.isInGraveyard(1, "Temporary Lockdown") shouldBe true
                withClue("nothing was exiled: the enchantment had already left") {
                    game.isOnBattlefield("Savannah Lions") shouldBe true
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Bonesplitter") shouldBe true
                    game.isOnBattlefield("Elite Vanguard") shouldBe true
                    game.isInExile(2, "Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
