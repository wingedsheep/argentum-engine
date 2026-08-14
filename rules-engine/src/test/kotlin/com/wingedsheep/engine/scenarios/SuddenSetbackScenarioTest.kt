package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mkm.cards.SuddenSetback
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sudden Setback (MKM #72) — {2}{U}{U} Instant.
 *
 * "The owner of target spell or nonland permanent puts it on their choice of the top or bottom of
 *  their library."
 *
 * A tuck that works on both halves of its union target, so both are exercised. Two details the
 * printed ruling pins down, and that a naive wiring would get backwards, get their own assertions:
 * the top-or-bottom choice belongs to the **owner** rather than to Sudden Setback's controller, and
 * a *spell* answered this way is removed from the stack without resolving — it is not a counter, so
 * the tucked spell never has its effect.
 */
class SuddenSetbackScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(SuddenSetback)

        context("Sudden Setback") {

            test("a nonland permanent goes to its owner's chosen top of library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sudden Setback")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpell(1, "Sudden Setback", targetId = courser).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val choice = game.getPendingDecision()
                withClue("resolution raises a top/bottom choice") {
                    (choice is ChooseOptionDecision) shouldBe true
                }
                val topBottom = choice as ChooseOptionDecision
                withClue("the choice belongs to the permanent's owner, not the caster") {
                    topBottom.playerId shouldBe game.player2Id
                }
                game.submitDecision(OptionChosenResponse(topBottom.id, 0)).error shouldBe null
                game.resolveStack()

                game.findPermanent("Centaur Courser") shouldBe null
                game.cardNameAtTop(2) shouldBe "Centaur Courser"
            }

            test("the owner may instead choose the bottom of their library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sudden Setback")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpell(1, "Sudden Setback", targetId = courser).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val topBottom = game.getPendingDecision() as ChooseOptionDecision
                game.submitDecision(OptionChosenResponse(topBottom.id, 1)).error shouldBe null
                game.resolveStack()

                game.findPermanent("Centaur Courser") shouldBe null
                withClue("Forest was the only library card, so it is on top and the Courser below") {
                    game.cardNameAtTop(2) shouldBe "Forest"
                    game.findCardsInLibrary(2, "Centaur Courser").isNotEmpty() shouldBe true
                }
            }

            test("a spell on the stack is tucked without resolving — not countered, but never resolves") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sudden Setback")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                // The caster keeps priority after casting; pass so player 1 can respond.
                game.passPriority()

                game.castSpellTargetingStackSpell(1, "Sudden Setback", "Grizzly Bears")
                    .error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val topBottom = game.getPendingDecision() as ChooseOptionDecision
                withClue("the spell's owner chooses, and it is player 2's spell") {
                    topBottom.playerId shouldBe game.player2Id
                }
                game.submitDecision(OptionChosenResponse(topBottom.id, 0)).error shouldBe null
                game.resolveStack()

                withClue("the creature spell never resolved — no Bears on the battlefield") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("and it is not in the graveyard either; it went to the library") {
                    game.cardNameAtTop(2) shouldBe "Grizzly Bears"
                }
            }
        }
    }

    private fun TestGame.cardNameAtTop(playerNumber: Int): String? =
        state.getEntity(
            state.getLibrary(if (playerNumber == 1) player1Id else player2Id).first()
        )?.get<CardComponent>()?.name
}
