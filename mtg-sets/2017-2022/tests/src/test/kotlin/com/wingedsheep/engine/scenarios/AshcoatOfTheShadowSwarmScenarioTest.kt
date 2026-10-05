package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ashcoat of the Shadow Swarm (J22 #19) — {3}{B} Legendary Creature — Rat Warlock, 3/4.
 *
 * "Whenever Ashcoat attacks or blocks, other Rats you control get +X/+X until end of turn, where X
 *  is the number of Rats you control.
 *  At the beginning of your end step, you may mill four cards. If you do, return up to two Rat
 *  creature cards from your graveyard to your hand."
 *
 * Covers that X counts every Rat you control (Ashcoat included, the opponent's Rats excluded), that
 * only *other* Rats you control are pumped, and that the end-step return can pick up a Rat card the
 * mill itself just put into the graveyard (ruling 2022-12-02).
 */
class AshcoatOfTheShadowSwarmScenarioTest : ScenarioTestBase() {

    private val rat = card("Test Rat") {
        manaCost = "{B}"
        typeLine = "Creature — Rat"
        power = 1
        toughness = 1
    }

    init {
        cardRegistry.register(rat)

        context("Ashcoat of the Shadow Swarm") {

            test("attacking pumps each other Rat you control by the number of Rats you control") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ashcoat of the Shadow Swarm")
                    .withCardOnBattlefield(1, "Test Rat")
                    .withCardOnBattlefield(1, "Test Rat")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Test Rat")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ashcoat = game.findPermanent("Ashcoat of the Shadow Swarm")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val aliceRats = game.findPermanents("Test Rat").filter {
                    game.state.projectedState.getController(it) == game.player1Id
                }
                val bobRat = game.findPermanents("Test Rat").single { it !in aliceRats }
                aliceRats.size shouldBe 2

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Ashcoat of the Shadow Swarm" to 2)).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("X = 3 (Ashcoat + two Rats; Bob's Rat doesn't count): each other Rat is 4/4") {
                    aliceRats.forEach {
                        projected.getPower(it) shouldBe 4
                        projected.getToughness(it) shouldBe 4
                    }
                }
                withClue("Ashcoat itself is not pumped") {
                    projected.getPower(ashcoat) shouldBe 3
                    projected.getToughness(ashcoat) shouldBe 4
                }
                withClue("non-Rats and the opponent's Rat are not pumped") {
                    projected.getPower(bears) shouldBe 2
                    projected.getPower(bobRat) shouldBe 1
                    projected.getToughness(bobRat) shouldBe 1
                }
            }

            test("end step: mill four, then return a Rat card that was just milled") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ashcoat of the Shadow Swarm")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Test Rat")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true).error shouldBe null

                withClue("the mill happened before the choice: the Rat is now in the graveyard") {
                    game.librarySize(1) shouldBe 1
                    game.isInGraveyard(1, "Test Rat") shouldBe true
                }
                val choice = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                val milledRat = game.findCardsInGraveyard(1, "Test Rat").single()
                withClue("only the Rat creature card is offered as returnable") {
                    choice.options.filter {
                        it in game.state.getGraveyard(game.player1Id)
                    } shouldBe listOf(milledRat)
                }
                game.selectCards(listOf(milledRat)).error shouldBe null
                game.resolveStack()

                withClue("the freshly milled Rat came back to hand; the Swamps stay in the graveyard") {
                    game.isInHand(1, "Test Rat") shouldBe true
                    game.isInGraveyard(1, "Test Rat") shouldBe false
                    game.findCardsInGraveyard(1, "Swamp").size shouldBe 3
                }
            }

            test("declining the mill mills nothing and returns nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Ashcoat of the Shadow Swarm")
                    .withCardInGraveyard(1, "Test Rat")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.librarySize(1) shouldBe 4
                withClue("'If you do' — no mill, so the Rat already in the graveyard stays there") {
                    game.isInGraveyard(1, "Test Rat") shouldBe true
                    game.isInHand(1, "Test Rat") shouldBe false
                }
            }
        }
    }
}
