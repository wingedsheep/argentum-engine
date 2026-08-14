package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Stirring Honormancer. */
class StirringHonormancerScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun TestGame.findExileCopy(name: String): com.wingedsheep.sdk.model.EntityId? =
        state.getExile(player1Id).firstOrNull { id ->
            state.getEntity(id)?.get<CardComponent>()?.name == name
        }

    init {
        context("Stirring Honormancer") {

            test("ETB looks at top X cards (X = creatures you control), one to hand, rest to graveyard") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Stirring Honormancer")
                    // {2}{W}{W/B}{B} needs white and black mana.
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    // Two other creatures already in play → X = 3 once the Honormancer resolves.
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(6) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(6) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val handBefore = game.handSize(1)
                val graveBefore = game.state.getGraveyard(game.player1Id).size

                game.castSpell(1, "Stirring Honormancer")
                game.resolveStack()

                // Resolve the creature, let its ETB trigger go on the stack and resolve until the
                // pipeline pauses to let the controller pick one of the X looked-at cards.
                var pick: com.wingedsheep.engine.core.SelectCardsDecision? = null
                var safety = 0
                while (pick == null && safety++ < 10) {
                    val pending = game.state.pendingDecision
                    if (pending is com.wingedsheep.engine.core.SelectCardsDecision) {
                        pick = pending
                        break
                    }
                    if (game.state.priorityPlayerId != null) game.passPriority() else break
                }
                withClue("Stirring Honormancer should pause to choose a card to keep") {
                    pick shouldNotBe null
                }
                withClue("X = 3 cards are looked at (two Bears + the Honormancer)") {
                    pick!!.options.size shouldBe 3
                }
                game.selectCards(listOf(pick!!.options.first()))
                game.resolveStack()

                // X = 3 (two Bears + the Honormancer). One card → hand, the other two → graveyard.
                withClue("One looked-at card goes to hand (net +1 vs the card cast)") {
                    // Casting the Honormancer removed it from hand; the ETB then adds 1 card.
                    game.handSize(1) shouldBe handBefore - 1 + 1
                }
                withClue("The remaining looked-at cards (X-1 = 2) go to the graveyard") {
                    game.state.getGraveyard(game.player1Id).size shouldBe graveBefore + 2
                }
            }
        }
    }
}
