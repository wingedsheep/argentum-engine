package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.m20.cards.AetherGust
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Aether Gust (M20 #42) — {1}{U} Instant.
 *
 * "Choose target spell or permanent that's red or green. Its owner puts it on their choice of the
 *  top or bottom of their library."
 *
 * The tuck itself is Sudden Setback's primitive; what is new here is the red-or-green restriction,
 * which is carried separately on both halves of the union target — so both halves are exercised,
 * and a permanent of neither colour must be rejected as a target.
 */
class AetherGustScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(AetherGust)

        context("Aether Gust") {

            test("a red permanent goes to its owner's chosen top of library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Aether Gust")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Aether Gust", targetId = giant).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val topBottom = game.getPendingDecision() as ChooseOptionDecision
                withClue("the choice belongs to the permanent's owner, not the caster") {
                    topBottom.playerId shouldBe game.player2Id
                }
                game.submitDecision(OptionChosenResponse(topBottom.id, 0)).error shouldBe null
                game.resolveStack()

                game.findPermanent("Hill Giant") shouldBe null
                game.cardNameAtTop(2) shouldBe "Hill Giant"
            }

            test("a permanent that is neither red nor green can't be targeted") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Aether Gust")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Wind Drake")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val drake = game.findPermanent("Wind Drake")!!
                game.castSpell(1, "Aether Gust", targetId = drake).error shouldNotBe null
                game.findPermanent("Wind Drake") shouldNotBe null
            }

            test("a green spell is tucked to the bottom without resolving") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Aether Gust")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.passPriority()

                game.castSpellTargetingStackSpell(1, "Aether Gust", "Grizzly Bears").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val topBottom = game.getPendingDecision() as ChooseOptionDecision
                topBottom.playerId shouldBe game.player2Id
                game.submitDecision(OptionChosenResponse(topBottom.id, 1)).error shouldBe null
                game.resolveStack()

                withClue("the creature spell never resolved") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                }
                withClue("Forest stays on top; the Bears went beneath it") {
                    game.cardNameAtTop(2) shouldBe "Forest"
                    game.findCardsInLibrary(2, "Grizzly Bears").isNotEmpty() shouldBe true
                }
            }
        }
    }

    private fun TestGame.cardNameAtTop(playerNumber: Int): String? =
        state.getEntity(
            state.getLibrary(if (playerNumber == 1) player1Id else player2Id).first()
        )?.get<CardComponent>()?.name
}
