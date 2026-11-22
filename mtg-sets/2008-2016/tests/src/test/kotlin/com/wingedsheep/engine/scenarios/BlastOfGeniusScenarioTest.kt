package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Blast of Genius — "Choose any target. Draw three cards, then discard a card. Blast of Genius
 * deals damage equal to the discarded card's mana value to that permanent or player."
 *
 * The damage reads the card discarded on resolution: a mana-value-4 card deals 4 to the
 * cast-time target, a land deals nothing.
 */
class BlastOfGeniusScenarioTest : ScenarioTestBase() {

    init {
        fun board(): TestGame = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Blast of Genius")
            .withCardInHand(1, "Hill Giant")
            .withLandsOnBattlefield(1, "Island", 3)
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.castAndDiscard(discard: String) {
            castSpellTargetingPlayer(1, "Blast of Genius", 2).error shouldBe null
            resolveStack()
            val decision = getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            val pick = decision.options.first { state.getEntity(it)?.get<CardComponent>()?.name == discard }
            selectCards(listOf(pick))
            if (state.stack.isNotEmpty()) resolveStack()
        }

        test("discarding a mana value 4 card deals 4 damage to the target") {
            val game = board()
            game.castAndDiscard("Hill Giant")
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.getLifeTotal(2) shouldBe 16
            game.state.getHand(game.player1Id).size shouldBe 3
        }

        test("discarding a land deals no damage") {
            val game = board()
            game.castAndDiscard("Forest")
            game.isInGraveyard(1, "Forest") shouldBe true
            game.getLifeTotal(2) shouldBe 20
            game.isInHand(1, "Hill Giant") shouldBe true
        }
    }
}
