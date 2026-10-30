package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Underworld Sentinel (THB #293) — {3}{B}{B} Creature — Skeleton Soldier, 4/5.
 *
 * "Whenever this creature attacks, exile target creature card from your graveyard.
 *  When this creature dies, put all cards exiled with it onto the battlefield."
 *
 * Pins the linked-exile pair: the attack trigger only targets creature cards in your own graveyard,
 * the cards it exiles return under your control when the Sentinel dies, and a Sentinel that leaves
 * the battlefield without dying abandons its pile (ruling).
 */
class UnderworldSentinelScenarioTest : ScenarioTestBase() {

    private fun sentinelGame(removal: String) = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Underworld Sentinel", summoningSickness = false)
        .withCardInGraveyard(1, "Grizzly Bears")
        .withCardInGraveyard(1, "Hill Giant")
        .withCardInGraveyard(1, "Swamp")
        .withCardInGraveyard(2, "Grizzly Bears")
        .withCardInHand(1, removal)
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attackAndExileBears() {
        advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Underworld Sentinel" to 2)).error shouldBe null

        val myBears = findCardsInGraveyard(1, "Grizzly Bears").single()
        val myGiant = findCardsInGraveyard(1, "Hill Giant").single()
        val mySwamp = findCardsInGraveyard(1, "Swamp").single()
        val theirBears = findCardsInGraveyard(2, "Grizzly Bears").single()
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets.values.flatten()
        withClue("only creature cards in your own graveyard can be targeted") {
            legal.toSet() shouldBe setOf(myBears, myGiant)
            (mySwamp in legal) shouldBe false
            (theirBears in legal) shouldBe false
        }
        selectTargets(listOf(myBears)).error shouldBe null
        resolveStack()

        withClue("the targeted creature card is exiled") {
            isInExile(1, "Grizzly Bears") shouldBe true
            isInGraveyard(1, "Hill Giant") shouldBe true
        }
    }

    init {
        context("Underworld Sentinel") {

            test("when it dies, the cards exiled with it enter the battlefield under your control") {
                val game = sentinelGame(removal = "Murder")
                game.attackAndExileBears()

                game.castSpell(1, "Murder", game.findPermanent("Underworld Sentinel")).error shouldBe null
                game.resolveStack()
                game.resolveStack()

                withClue("the Sentinel died") { game.isInGraveyard(1, "Underworld Sentinel") shouldBe true }
                val bears = game.findPermanent("Grizzly Bears")
                withClue("the exiled Grizzly Bears is back on the battlefield") { (bears != null) shouldBe true }
                game.state.projectedState.getController(bears!!) shouldBe game.player1Id
                game.isInExile(1, "Grizzly Bears") shouldBe false
                withClue("cards it never exiled stay where they were") {
                    game.isInGraveyard(1, "Hill Giant") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
            }

            test("leaving the battlefield without dying leaves the exiled cards in exile") {
                val game = sentinelGame(removal = "Unsummon")
                game.attackAndExileBears()

                game.castSpell(1, "Unsummon", game.findPermanent("Underworld Sentinel")).error shouldBe null
                game.resolveStack()

                withClue("the Sentinel was bounced, not killed") { game.isInHand(1, "Underworld Sentinel") shouldBe true }
                withClue("its pile stays exiled for the rest of the game (ruling)") {
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
            }

            test("it can attack with no creature card in your graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Underworld Sentinel", summoningSickness = false)
                    .withCardInGraveyard(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Underworld Sentinel" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the trigger has no legal target and is removed; nothing is exiled") {
                    game.hasPendingDecision() shouldBe false
                    game.isInGraveyard(1, "Swamp") shouldBe true
                }
            }
        }
    }
}
