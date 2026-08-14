package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Fanatic of the Harrowing. */
class FanaticOfTheHarrowingScenarioTest : ScenarioTestBase() {

    init {
        context("Fanatic of the Harrowing — ETB discard then draw") {
            test("each player discards a card and the controller draws back when they discarded") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fanatic of the Harrowing")
                    .withCardInHand(1, "Swamp")           // controller's discard fodder
                    .withCardInHand(2, "Forest")          // opponent's discard fodder
                    .withCardInLibrary(1, "Mountain")     // controller's replacement draw
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Fanatic of the Harrowing").error shouldBe null
                game.resolveStack()

                // Discards may pause for a card selection per player; auto-resolve single options.
                var guard = 0
                while (game.hasPendingDecision() && guard < 20) {
                    val decision = game.state.pendingDecision
                    if (decision is ChooseTargetsDecision) break
                    // SelectCards decisions: each player has exactly one discardable card here,
                    // so feeding it the only option is deterministic.
                    val p1 = com.wingedsheep.sdk.model.EntityId.of("player-1")
                    val p2 = com.wingedsheep.sdk.model.EntityId.of("player-2")
                    val swamp = game.state.getHand(p1).firstOrNull { id ->
                        game.state.getEntity(id)
                            ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Swamp"
                    }
                    val forest = game.state.getHand(p2).firstOrNull { id ->
                        game.state.getEntity(id)
                            ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Forest"
                    }
                    when {
                        swamp != null -> game.selectCards(listOf(swamp))
                        forest != null -> game.selectCards(listOf(forest))
                        else -> game.resolveStack()
                    }
                    game.resolveStack()
                    guard++
                }

                withClue("controller discarded the Swamp") { game.isInGraveyard(1, "Swamp") shouldBe true }
                withClue("opponent discarded the Forest") { game.isInGraveyard(2, "Forest") shouldBe true }
                withClue("controller drew a replacement (the Mountain)") {
                    game.isInHand(1, "Mountain") shouldBe true
                }
            }
        }
    }
}
