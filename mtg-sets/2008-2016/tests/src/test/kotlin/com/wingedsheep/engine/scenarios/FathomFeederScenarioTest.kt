package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.bfz.cards.FathomFeeder
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fathom Feeder (BFZ #203): "{3}{U}{B}: Draw a card. Each opponent exiles the top card of their
 * library."
 *
 * The activation draws for its controller and puts each opponent's top card — and only the top
 * card — face up into that opponent's own exile, where processors look for it. An opponent with
 * an empty library exiles nothing and doesn't lose.
 */
class FathomFeederScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = FathomFeeder.activatedAbilities.first().id

        fun activate(game: TestGame) {
            val feeder = game.findPermanent("Fathom Feeder")!!
            val result = game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = feeder, abilityId = abilityId),
            )
            withClue("${result.error}") { result.error shouldBe null }
            if (game.getPendingDecision() is SelectManaSourcesDecision) {
                game.submitManaSourcesAutoPay()
            }
            game.resolveStack()
        }

        test("draws a card and exiles the top card of the opponent's library into their exile") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Fathom Feeder")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Mountain") // top
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val handBefore = game.handSize(1)

            activate(game)

            game.handSize(1) shouldBe handBefore + 1
            game.findCardsInHand(1, "Forest").size shouldBe 1
            game.isInExile(2, "Mountain") shouldBe true
            game.isInExile(1, "Mountain") shouldBe false
            game.librarySize(2) shouldBe 1
            game.findCardsInLibrary(2, "Plains").size shouldBe 1
            game.isInExile(1, "Forest") shouldBe false
        }

        test("an opponent with an empty library exiles nothing and the draw still happens") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Fathom Feeder")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val handBefore = game.handSize(1)

            activate(game)

            game.handSize(1) shouldBe handBefore + 1
            game.state.getExile(game.player2Id).size shouldBe 0
            game.state.gameOver shouldBe false
        }
    }
}
