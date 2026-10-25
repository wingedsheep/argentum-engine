package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Amulet of Vigor — "Whenever a permanent you control enters tapped, untap it."
 *
 * A tapland played under it comes back untapped; a land that enters untapped doesn't trigger it;
 * with two Amulets both trigger, the first untaps and the second finds nothing to do (ruling).
 */
class AmuletOfVigorScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)?.get<TappedComponent>() != null

    private fun TestGame.play(playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        val land = findCardsInHand(playerNumber, name).first()
        withClue("playing $name is legal") { execute(PlayLand(playerId, land)).error shouldBe null }
        return land
    }

    private fun base(amulets: Int = 1) = scenario()
        .withPlayers("Player1", "Player2")
        .apply { repeat(amulets) { withCardOnBattlefield(1, "Amulet of Vigor") } }
        .withCardInHand(1, "Golgari Guildgate")
        .withCardInHand(1, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Amulet of Vigor") {

            test("a land that enters tapped is untapped by the trigger") {
                val game = base().build()
                val gate = game.play(1, "Golgari Guildgate")

                withClue("Golgari Guildgate enters tapped and Amulet's trigger is on the stack") {
                    game.isTapped(gate) shouldBe true
                    game.state.stack.size shouldBe 1
                }
                game.resolveStack()
                game.isTapped(gate) shouldBe false
            }

            test("a land that enters untapped does not trigger it") {
                val game = base().build()
                game.play(1, "Swamp")
                game.state.stack.size shouldBe 0
            }

            test("two Amulets both trigger; the first untaps it and the second does nothing") {
                val game = base(amulets = 2).build()
                val gate = game.play(1, "Golgari Guildgate")

                game.state.stack.size shouldBe 2
                game.resolveStack()
                game.isTapped(gate) shouldBe false
                game.state.stack.size shouldBe 0
            }
        }
    }
}
