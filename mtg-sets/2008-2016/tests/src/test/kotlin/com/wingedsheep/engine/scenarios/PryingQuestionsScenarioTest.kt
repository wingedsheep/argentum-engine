package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Prying Questions (EMN #101) — {2}{B} Sorcery.
 *
 *   Target opponent loses 3 life and puts a card from their hand on top of their library.
 *
 * The claim under test is *who decides*: the selection is addressed to the targeted opponent
 * (`Chooser.TargetPlayer`), not the caster, and the chosen card lands on top of the opponent's
 * own library. An empty hand still costs the opponent 3 life (ruling 2016-07-13).
 */
class PryingQuestionsScenarioTest : ScenarioTestBase() {

    init {
        test("the targeted opponent loses 3 life and chooses a card to put on top of their library") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Prying Questions")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpellTargetingPlayer(1, "Prying Questions", 2).error shouldBe null
            game.resolveStack()

            withClue("the opponent, not the caster, chooses the card") {
                game.hasPendingDecision() shouldBe true
                game.state.pendingDecision!!.playerId shouldBe game.player2Id
            }
            val hillGiant = game.findCardsInHand(2, "Hill Giant").single()
            game.selectCards(listOf(hillGiant)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 20
            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.isInHand(2, "Hill Giant") shouldBe false
            withClue("the chosen card is on top of the opponent's library") {
                game.state.getLibrary(game.player2Id).first() shouldBe hillGiant
            }
        }

        test("an opponent with an empty hand just loses 3 life") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Prying Questions")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.handSize(2) shouldBe 0
            game.castSpellTargetingPlayer(1, "Prying Questions", 2).error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.getLifeTotal(2) shouldBe 17
            game.librarySize(2) shouldBe 1
        }
    }
}
