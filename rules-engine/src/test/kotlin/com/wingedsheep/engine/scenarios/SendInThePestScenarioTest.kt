package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Send in the Pest (Secrets of Strixhaven #100).
 *
 * Send in the Pest ({1}{B}, Sorcery):
 *   Each opponent discards a card. You create a 1/1 black and green Pest creature token
 *   with "Whenever this token attacks, you gain 1 life."
 *
 * Exercises the each-opponent-discards effect, token creation, and the token's own
 * granted attack-trigger life gain.
 */
class SendInThePestScenarioTest : ScenarioTestBase() {

    private fun life(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<LifeTotalComponent>()?.life ?: 0

    init {
        context("Send in the Pest — discard + Pest token") {

            test("each opponent discards, a Pest token is created, and it gains life on attack") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Send in the Pest")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardInHand(2, "Grizzly Bears") // the single card the opponent must discard
                    .withCardInLibrary(1, "Swamp")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val player1 = game.state.activePlayerId!!
                val startLife = life(game, player1)

                val cast = game.castSpell(1, "Send in the Pest")
                withClue("Casting Send in the Pest should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack() // resolves up to the opponent's discard decision

                // Opponent discards their only card (auto-forced when it's their only card).
                if (game.hasPendingDecision()) {
                    val toDiscard = game.findCardsInHand(2, "Grizzly Bears")
                    game.selectCards(toDiscard)
                    game.resolveStack()
                }

                withClue("Opponent discarded their card") { game.handSize(2) shouldBe 0 }
                withClue("Grizzly Bears is in the opponent's graveyard") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }

                val pest = game.findPermanent("Pest Token")
                withClue("A Pest token should have been created") { (pest != null) shouldBe true }

                // Move to combat and attack with the Pest; its attack trigger gains 1 life.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Pest Token" to 2))
                game.resolveStack()

                withClue("Attacking with the Pest gains its controller 1 life") {
                    life(game, player1) shouldBe startLife + 1
                }
            }
        }
    }
}
