package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dubious Challenge (KLD #152) — "Look at the top ten cards of your library, exile up to two
 * creature cards from among them, then shuffle. Target opponent may choose one of the exiled
 * cards and put it onto the battlefield under their control. Put the rest onto the battlefield
 * under your control."
 *
 * Covers who decides each step — the caster picks what to exile, the targeted opponent picks
 * from the exiled cards — and who ends up controlling what, including the opponent declining.
 */
class DubiousChallengeScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Dubious Challenge")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Hill Giant")
        .withCardInLibrary(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.optionNamed(decision: SelectCardsDecision, name: String): EntityId =
        decision.options.first { state.getEntity(it)?.get<CardComponent>()?.name == name }

    init {
        context("Dubious Challenge") {

            test("caster exiles two creatures; the opponent takes one, the caster gets the other") {
                val game = board()
                game.castSpellTargetingPlayer(1, "Dubious Challenge", 2).error shouldBe null
                game.resolveStack()

                val exileChoice = game.getPendingDecision()
                exileChoice.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the caster chooses what to exile") {
                    exileChoice.playerId shouldBe game.player1Id
                }
                game.selectCards(
                    listOf(game.optionNamed(exileChoice, "Grizzly Bears"), game.optionNamed(exileChoice, "Hill Giant"))
                ).error shouldBe null

                val opponentChoice = game.getPendingDecision()
                opponentChoice.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the targeted opponent chooses among the exiled cards") {
                    opponentChoice.playerId shouldBe game.player2Id
                    opponentChoice.options.size shouldBe 2
                    opponentChoice.minSelections shouldBe 0
                    opponentChoice.maxSelections shouldBe 1
                }
                game.selectCards(listOf(game.optionNamed(opponentChoice, "Hill Giant"))).error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                val giant = game.findPermanent("Hill Giant")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the opponent controls the card they chose") {
                    projected.getController(giant) shouldBe game.player2Id
                }
                withClue("the caster controls the rest") {
                    projected.getController(bears) shouldBe game.player1Id
                }
                withClue("the unexiled card was shuffled back, not moved") {
                    game.librarySize(1) shouldBe 1
                }
            }

            test("the opponent may decline, so every exiled card enters under the caster's control") {
                val game = board()
                game.castSpellTargetingPlayer(1, "Dubious Challenge", 2).error shouldBe null
                game.resolveStack()

                val exileChoice = game.getPendingDecision()
                exileChoice.shouldBeInstanceOf<SelectCardsDecision>()
                game.selectCards(listOf(game.optionNamed(exileChoice, "Hill Giant"))).error shouldBe null

                val opponentChoice = game.getPendingDecision()
                opponentChoice.shouldBeInstanceOf<SelectCardsDecision>()
                opponentChoice.playerId shouldBe game.player2Id
                game.skipSelection().error shouldBe null
                game.resolveStack()

                val giant = game.findPermanent("Hill Giant")!!
                game.state.projectedState.getController(giant) shouldBe game.player1Id
                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.librarySize(1) shouldBe 2
            }
        }
    }
}
