package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Firestorm Phoenix (LEG #147) — "If this creature would die, return it to its owner's hand
 * instead. Until that player's next turn, that player plays with that card revealed in their hand
 * and can't play it."
 */
class FirestormPhoenixScenarioTest : ScenarioTestBase() {

    init {
        context("Firestorm Phoenix") {

            test("dies -> back to hand, revealed to the opponent, uncastable until its owner's next turn") {
                val builder = scenario()
                    .withPlayers("Phoenix Player", "Opponent")
                    .withCardOnBattlefield(1, "Firestorm Phoenix")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) {
                    builder.withCardInLibrary(1, "Mountain")
                    builder.withCardInLibrary(2, "Mountain")
                }
                val game = builder.build()
                val phoenix = game.findPermanent("Firestorm Phoenix")!!

                game.castSpell(1, "Lightning Bolt", phoenix).error shouldBe null
                game.resolveStack()

                withClue("it returns to its owner's hand instead of dying") {
                    game.state.getHand(game.player1Id) shouldContain phoenix
                    game.isInGraveyard(1, "Firestorm Phoenix") shouldBe false
                }
                withClue("the opponent sees the card in that hand") {
                    game.getClientState(2).cards[phoenix]?.name shouldBe "Firestorm Phoenix"
                }
                withClue("it isn't offered as a cast, and a submitted cast is rejected") {
                    game.getLegalActions(1).none { (it.action as? CastSpell)?.cardId == phoenix } shouldBe true
                    game.castSpell(1, "Firestorm Phoenix").error shouldNotBe null
                }

                // The opponent's turn: still revealed.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player2Id
                withClue("still revealed during the opponent's turn") {
                    game.getClientState(2).cards[phoenix]?.name shouldBe "Firestorm Phoenix"
                }

                // Its owner's next turn: castable again.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id
                withClue("castable again on its owner's next turn") {
                    game.getLegalActions(1).any { (it.action as? CastSpell)?.cardId == phoenix } shouldBe true
                    game.castSpell(1, "Firestorm Phoenix").error shouldBe null
                }
                game.resolveStack()
                game.findPermanent("Firestorm Phoenix") shouldBe phoenix
            }
        }
    }
}
