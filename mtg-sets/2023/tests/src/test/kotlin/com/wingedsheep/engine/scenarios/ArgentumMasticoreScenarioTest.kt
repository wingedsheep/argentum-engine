package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Argentum Masticore — {5} Artifact Creature — Phyrexian Masticore 5/5 (ONE #222).
 *
 * "First strike, protection from multicolored
 *  At the beginning of your upkeep, sacrifice this creature unless you discard a card. When you
 *  discard a card this way, destroy target nonland permanent an opponent controls with mana value
 *  less than or equal to the mana value of the discarded card."
 */
class ArgentumMasticoreScenarioTest : ScenarioTestBase() {

    /** P2's main phase → P1's upkeep, with the Masticore's trigger on the stack and resolving. */
    private fun ScenarioBuilder.toMastUpkeep(): TestGame {
        val game = withCardInLibrary(1, "Plains")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.ENDING, Step.END)
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.resolveStack()
        return game
    }

    init {
        context("protection from multicolored") {
            test("a multicolored spell can't target it; a monocolored one can") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(2, "Argentum Masticore")
                    .withCardInHand(1, "Lightning Helix")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val masticore = game.findPermanent("Argentum Masticore")!!

                game.castSpell(1, "Lightning Helix", masticore).error shouldNotBe null
                game.castSpell(1, "Lightning Bolt", masticore).error shouldBe null
                game.resolveStack()

                // 3 damage to a 5/5 — it survives, but the monocolored spell did resolve at it.
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.isOnBattlefield("Argentum Masticore") shouldBe true
            }

            test("a multicolored creature can't block it; a monocolored creature can") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Argentum Masticore", summoningSickness = false)
                    .withCardOnBattlefield(2, "Watchwolf")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Argentum Masticore" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                game.declareBlockers(mapOf("Watchwolf" to listOf("Argentum Masticore"))).error shouldNotBe null
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Argentum Masticore"))).error shouldBe null
            }
        }

        context("upkeep: sacrifice unless you discard") {
            test("discarding destroys an opponent's nonland permanent with mana value up to the discarded card's") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Argentum Masticore")
                    .withCardInHand(1, "Grizzly Bears") // mana value 2
                    .withCardOnBattlefield(2, "Grizzly Bears") // mana value 2 — legal
                    .withCardOnBattlefield(2, "Serra Angel") // mana value 5 — too big
                    .toMastUpkeep()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)

                // A one-card hand auto-selects the discard; a larger hand would prompt.
                if (game.getPendingDecision() is SelectCardsDecision) {
                    game.selectCards(game.findCardsInHand(1, "Grizzly Bears"))
                }

                // The reflexive trigger chooses its target as it goes on the stack.
                val choose = game.getPendingDecision()
                choose.shouldBeInstanceOf<ChooseTargetsDecision>()
                val bears = game.findAllPermanents("Grizzly Bears").single()
                val angel = game.findPermanent("Serra Angel")!!
                val legal = choose.legalTargets.values.flatten()
                legal shouldBe listOf(bears)
                (angel in legal) shouldBe false

                game.selectTargets(listOf(bears))
                game.resolveStack()

                game.isOnBattlefield("Argentum Masticore") shouldBe true
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true // the discarded card
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true // destroyed
                game.isOnBattlefield("Serra Angel") shouldBe true
            }

            test("declining to discard sacrifices it") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Argentum Masticore")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .toMastUpkeep()

                game.answerYesNo(false)
                game.resolveStack()

                game.isInGraveyard(1, "Argentum Masticore") shouldBe true
                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }

            test("with an empty hand it is sacrificed without a prompt") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Argentum Masticore")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .toMastUpkeep()

                game.getPendingDecision() shouldBe null
                game.isInGraveyard(1, "Argentum Masticore") shouldBe true
            }

            test("discarding keeps it even with no legal target for the reflexive trigger") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Argentum Masticore")
                    .withCardInHand(1, "Plains") // mana value 0
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .toMastUpkeep()

                game.answerYesNo(true)
                if (game.getPendingDecision() is SelectCardsDecision) {
                    game.selectCards(game.findCardsInHand(1, "Plains"))
                }
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                game.isOnBattlefield("Argentum Masticore") shouldBe true
                game.isInGraveyard(1, "Plains") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
        }
    }
}
