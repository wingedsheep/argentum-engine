package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Confounding Conundrum (ZNR #53) — "Whenever a land an opponent controls enters, if that player
 * had another land enter the battlefield under their control this turn, they return a land they
 * control to its owner's hand."
 *
 * Proves the first land of the turn is free, and that the second one makes *that opponent*
 * choose a land of theirs to bounce.
 */
class ConfoundingConundrumScenarioTest : ScenarioTestBase() {

    init {
        context("Confounding Conundrum") {

            test("an opponent's first land is free; their second makes them return a land") {
                val game = scenario()
                    .withPlayers("Conundrum", "Opponent")
                    .withCardOnBattlefield(1, "Confounding Conundrum")
                    .withCardOnBattlefield(2, "Forest")
                    .withCardOnBattlefield(2, "Island")
                    .withCardInHand(2, "Mountain")
                    .withCardInHand(2, "Swamp")
                    .withCardInHand(2, "Growth Spiral")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mountain = game.findCardsInHand(2, "Mountain").single()
                game.execute(PlayLand(game.player2Id, mountain)).error.shouldBeNull()

                withClue("first land this turn: no trigger") {
                    game.state.stack.size shouldBe 0
                    game.state.pendingDecision.shouldBeNull()
                }

                // Growth Spiral puts a second land onto the battlefield this turn.
                game.castSpell(2, "Growth Spiral").error.shouldBeNull()
                game.resolveStack()
                if (game.state.pendingDecision is YesNoDecision) game.answerYesNo(true)
                val swamp = game.findCardsInHand(2, "Swamp").single()
                game.selectCards(listOf(swamp)).error.shouldBeNull()
                game.resolveStack()

                val decision = game.state.pendingDecision
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the opponent whose land entered chooses the land to return") {
                    decision.playerId shouldBe game.player2Id
                }
                val forest = game.findPermanent("Forest")!!
                game.selectCards(listOf(forest)).error.shouldBeNull()
                game.resolveStack()

                withClue("the Forest is back in its owner's hand; the new Swamp stays") {
                    game.findCardsInHand(2, "Forest").size shouldBe 1
                    game.findPermanents("Swamp").size shouldBe 1
                }
            }
        }
    }
}
