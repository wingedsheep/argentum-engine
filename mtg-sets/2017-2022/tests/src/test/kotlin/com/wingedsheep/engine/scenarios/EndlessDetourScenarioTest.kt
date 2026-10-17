package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Endless Detour (SNC #183) — {G}{W}{U} Instant.
 *
 *   The owner of target spell, nonland permanent, or card in a graveyard puts it on their
 *   choice of the top or bottom of their library.
 *
 * One target from a three-zone union; each zone clause resolves through the same owner-chooses
 * top/bottom effect. Options are offered as [Top, Bottom].
 */
class EndlessDetourScenarioTest : ScenarioTestBase() {

    private fun answer(game: TestGame, optionIndex: Int) {
        val decision = game.getPendingDecision()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        game.submitDecision(OptionChosenResponse(decision.id, optionIndex))
    }

    init {
        context("Endless Detour") {

            test("an opponent's nonland permanent goes to the top of its owner's library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Endless Detour")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Endless Detour", bears).error shouldBe null
                game.resolveStack()

                withClue("the owner (Player2) makes the choice") {
                    (game.getPendingDecision() as ChooseOptionDecision).playerId shouldBe game.player2Id
                }
                answer(game, 0)

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.state.getLibrary(game.player2Id).first() shouldBe bears
            }

            test("a card in a graveyard goes to the bottom of its owner's library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Endless Detour")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findCardsInGraveyard(2, "Grizzly Bears").single()
                game.castSpellTargetingGraveyardCard(1, "Endless Detour", 2, "Grizzly Bears").error shouldBe null
                game.resolveStack()
                answer(game, 1)

                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                game.state.getLibrary(game.player2Id).last() shouldBe bears
            }

            test("a spell on the stack leaves the stack for its owner's library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Endless Detour")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                game.castSpellTargetingStackSpell(1, "Endless Detour", "Grizzly Bears").error shouldBe null
                game.resolveStack()
                answer(game, 0)
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.findCardsInLibrary(1, "Grizzly Bears").size shouldBe 1
                game.state.getLibrary(game.player1Id).first() shouldBe
                    game.findCardsInLibrary(1, "Grizzly Bears").single()
            }
        }
    }
}
