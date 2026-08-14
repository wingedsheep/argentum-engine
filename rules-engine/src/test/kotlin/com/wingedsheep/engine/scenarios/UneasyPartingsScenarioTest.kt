package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Uneasy Partings (HOB) — {3}{U} Instant.
 *
 * "This spell costs {1} less to cast if it targets an attacking nontoken creature.
 *  Target creature's owner puts it on their choice of the top or bottom of their library."
 *
 * The discount has two conjuncts (attacking *and* nontoken), so a non-attacking creature must not
 * discount it. Resolution hands the top/bottom choice to the creature's *owner*, not the caster.
 */
class UneasyPartingsScenarioTest : ScenarioTestBase() {

    private val calculator = CostCalculator(cardRegistry)

    init {
        context("Uneasy Partings") {

            test("with no target it costs the printed {3}{U} = 4") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Uneasy Partings")
                    .build()

                calculator.calculateEffectiveCost(
                    game.state, cardRegistry.requireCard("Uneasy Partings"), game.player1Id
                ).cmc shouldBe 4
            }

            test("targeting a non-attacking creature does not discount it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Uneasy Partings")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                calculator.calculateEffectiveCost(
                    game.state, cardRegistry.requireCard("Uneasy Partings"),
                    game.player1Id, listOf(courser)
                ).cmc shouldBe 4
            }

            test("targeting an attacking nontoken creature takes {1} off") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(2, "Uneasy Partings")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Centaur Courser" to 2)).error shouldBe null
                val courser = game.findPermanent("Centaur Courser")!!

                withClue("attacking and nontoken — the {1} discount applies") {
                    calculator.calculateEffectiveCost(
                        game.state, cardRegistry.requireCard("Uneasy Partings"),
                        game.player2Id, listOf(courser)
                    ).cmc shouldBe 3
                }
            }

            test("resolution lets the creature's owner put it on top of their library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Uneasy Partings")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpell(1, "Uneasy Partings", targetId = courser).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val choice = game.getPendingDecision()
                withClue("resolution raises a top/bottom choice") {
                    (choice is ChooseOptionDecision) shouldBe true
                }
                val topBottom = choice as ChooseOptionDecision
                withClue("the choice belongs to the creature's owner, not the caster") {
                    topBottom.playerId shouldBe game.player2Id
                }
                game.submitDecision(OptionChosenResponse(topBottom.id, 0)).error shouldBe null
                game.resolveStack()

                withClue("the creature left the battlefield for its owner's library") {
                    game.findPermanent("Centaur Courser") shouldBe null
                    game.findCardsInLibrary(2, "Centaur Courser").isNotEmpty() shouldBe true
                }
                withClue("it went to the top, as chosen") {
                    game.cardNameAtTop(2) shouldBe "Centaur Courser"
                }
            }

            test("the owner may instead choose the bottom of their library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Uneasy Partings")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpell(1, "Uneasy Partings", targetId = courser).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val choice = game.getPendingDecision() as ChooseOptionDecision
                game.submitDecision(OptionChosenResponse(choice.id, 1)).error shouldBe null
                game.resolveStack()

                withClue("choosing the bottom leaves the Forest on top") {
                    game.cardNameAtTop(2) shouldBe "Forest"
                    game.cardNameAtBottom(2) shouldBe "Centaur Courser"
                }
            }
        }
    }

    private fun TestGame.libraryIds(playerNumber: Int): List<com.wingedsheep.sdk.model.EntityId> =
        state.getLibrary(if (playerNumber == 1) player1Id else player2Id)

    private fun TestGame.cardNameAtTop(playerNumber: Int): String? =
        state.getEntity(libraryIds(playerNumber).first())?.get<CardComponent>()?.name

    private fun TestGame.cardNameAtBottom(playerNumber: Int): String? =
        state.getEntity(libraryIds(playerNumber).last())?.get<CardComponent>()?.name
}
