package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bristlebud Farmer (BIG #17) — {2}{G}{G} Creature — Plant Druid, 5/5, Trample.
 *
 * "When this creature enters, create two Food tokens.
 *  Whenever this creature attacks, you may sacrifice a Food. If you do, mill three cards. You may
 *  put a permanent card from among them into your hand."
 */
class BristlebudFarmerScenarioTest : ScenarioTestBase() {

    init {
        test("ETB creates two Food tokens") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Bristlebud Farmer")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInLibrary(1, "Forest")
                .build()

            game.castSpell(1, "Bristlebud Farmer").error shouldBe null
            game.resolveStack()

            withClue("ETB created two Food tokens") {
                foodCount(game) shouldBe 2
            }
        }

        test("attacking and sacrificing a Food mills three and recurs a permanent") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Bristlebud Farmer")
                .withCardOnBattlefield(1, "Food", isToken = true)
                .withCardInLibrary(1, "Grizzly Bears") // permanent to recur (top after the two Forests below)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .build()

            val graveBefore = game.graveyardSize(1)
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Bristlebud Farmer" to 2))
            game.resolveStack() // attack trigger goes on the stack; resolving pauses at the "may"

            // "You may sacrifice a Food" — yes.
            withClue("attack offers the optional Food sacrifice") {
                (game.getPendingDecision() is YesNoDecision) shouldBe true
            }
            game.answerYesNo(true)

            // Then "you may put a permanent card from among the milled three into your hand".
            withClue("a select-permanent-from-milled prompt is offered") {
                (game.getPendingDecision() is SelectCardsDecision) shouldBe true
            }
            val grizzly = game.findCardsInLibrary(1, "Grizzly Bears")
            // The milled cards moved to graveyard; recover the Grizzly Bears from there.
            val grizzlyInGrave = game.findCardsInGraveyard(1, "Grizzly Bears")
            game.selectCards(if (grizzlyInGrave.isNotEmpty()) grizzlyInGrave else grizzly)
            game.resolveStack()

            withClue("the recurred permanent is now in hand") {
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }
            withClue("the Food was sacrificed") {
                foodCount(game) shouldBe 0
            }
            withClue("three milled, one recurred to hand → net +2 in graveyard") {
                (game.graveyardSize(1) - graveBefore) shouldBe 2
            }
        }

        test("declining the attack 'may' leaves Food and library untouched") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Bristlebud Farmer")
                .withCardOnBattlefield(1, "Food", isToken = true)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .build()

            val graveBefore = game.graveyardSize(1)
            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Bristlebud Farmer" to 2))
            game.resolveStack()

            (game.getPendingDecision() is YesNoDecision) shouldBe true
            game.answerYesNo(false)
            game.resolveStack()

            withClue("Food remains; nothing milled") {
                foodCount(game) shouldBe 1
                game.graveyardSize(1) shouldBe graveBefore
            }
        }
    }

    private fun foodCount(game: TestGame): Int =
        game.state.getBattlefield(game.player1Id).count { id ->
            game.state.getEntity(id)?.get<CardComponent>()
                ?.typeLine?.subtypes?.any { it.value == "Food" } == true
        }
}
