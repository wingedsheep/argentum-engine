package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Confusticate and Bebother (HOB #35) — {2}{U} Instant.
 *
 * "Choose one —
 *  • Counter target spell unless its controller pays {4}.
 *  • Draw two cards, then discard a card."
 *
 * Mode 0 is exercised in both directions (the controller declining to pay and paying), and
 * mode 1 for its ordering — draw two *then* discard one, netting +1 card.
 */
class ConfusticateAndBebotherScenarioTest : ScenarioTestBase() {

    init {
        context("Confusticate and Bebother") {

            test("mode 0 — the spell is countered when its controller declines to pay {4}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Confusticate and Bebother")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInHand(1, "Centaur Courser")
                    // Three lands for the Courser plus four more, so declining is a real choice.
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Centaur Courser").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                // The caster keeps priority after casting; pass it so the opponent may respond.
                game.passPriority().error shouldBe null

                val courserOnStack = game.state.stack.single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Centaur Courser"
                }
                val counterSpell = game.state.getHand(game.player2Id).single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Confusticate and Bebother"
                }
                val target = listOf(ChosenTarget.Spell(courserOnStack))

                game.execute(
                    CastSpell(
                        game.player2Id, counterSpell, target,
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(target)
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val pay = game.getPendingDecision()
                withClue("the targeted spell's controller is offered the {4} tax") {
                    (pay is YesNoDecision) shouldBe true
                    pay!!.playerId shouldBe game.player1Id
                }
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("declining the tax counters the spell into its owner's graveyard") {
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                    game.isInGraveyard(1, "Centaur Courser") shouldBe true
                }
            }

            test("mode 0 — the spell resolves when its controller pays {4}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Confusticate and Bebother")
                    .withLandsOnBattlefield(2, "Island", 3)
                    .withCardInHand(1, "Centaur Courser")
                    // Three lands for the Courser plus four more to pay the tax.
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Centaur Courser").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                // The caster keeps priority after casting; pass it so the opponent may respond.
                game.passPriority().error shouldBe null

                val courserOnStack = game.state.stack.single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Centaur Courser"
                }
                val counterSpell = game.state.getHand(game.player2Id).single { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Confusticate and Bebother"
                }
                val target = listOf(ChosenTarget.Spell(courserOnStack))

                game.execute(
                    CastSpell(
                        game.player2Id, counterSpell, target,
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(target)
                    )
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                (game.getPendingDecision() is YesNoDecision) shouldBe true
                game.answerYesNo(true).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("paying the {4} lets the Courser resolve") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
            }

            test("mode 1 — draws two then discards one, netting a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Confusticate and Bebother")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val spell = game.state.getHand(game.player1Id).single()
                val libraryBefore = game.librarySize(1)

                game.execute(
                    CastSpell(game.player1Id, spell, emptyList(), chosenModes = listOf(1))
                ).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val discard = game.getPendingDecision()
                withClue("after drawing two, the caster picks a card to discard") {
                    (discard is SelectCardsDecision) shouldBe true
                }
                val discardOptions = (discard as SelectCardsDecision).options
                withClue("the two freshly drawn cards are the choices") {
                    discardOptions.size shouldBe 2
                }
                game.selectCards(listOf(discardOptions.first())).error shouldBe null
                game.resolveStack()

                withClue("two cards left the library") {
                    game.librarySize(1) shouldBe libraryBefore - 2
                }
                withClue("drew 2 and discarded 1 — net +1 card in hand") {
                    game.handSize(1) shouldBe 1
                }
                withClue("the discarded card is in the graveyard alongside the spell") {
                    game.graveyardSize(1) shouldBe 2
                }
            }
        }
    }
}
