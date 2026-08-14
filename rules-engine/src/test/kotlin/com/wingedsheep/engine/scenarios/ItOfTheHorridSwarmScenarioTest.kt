package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for It of the Horrid Swarm — {8} 4/4 Eldrazi Insect with emerge {6}{G} and
 * "When you cast this spell, create two 1/1 green Insect creature tokens."
 *
 * The tokens come from a cast trigger, so they arrive while the spell is still on the stack.
 */
class ItOfTheHorridSwarmScenarioTest : ScenarioTestBase() {

    init {
        context("It of the Horrid Swarm") {

            test("emerge cast makes two 1/1 Insect tokens before the body resolves") {
                val game = scenario()
                    .withPlayers()
                    .withCardInHand(1, "It of the Horrid Swarm")
                    .withCardOnBattlefield(1, "Force of Nature") // {3}{G}{G} → mana value 5
                    // Emerge {6}{G} reduced by 5 → {1}{G}: two Forests is exactly enough.
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .build()

                val cast = game.castSpellWithEmerge(1, "It of the Horrid Swarm", "Force of Nature")
                withClue("the emerge cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.isInGraveyard(1, "Force of Nature") shouldBe true

                game.resolveStack()

                val tokens = game.findAllPermanents("Insect Token")
                withClue("the cast trigger created two Insect tokens") { tokens.size shouldBe 2 }
                for (token in tokens) {
                    game.state.projectedState.getPower(token) shouldBe 1
                    game.state.projectedState.getToughness(token) shouldBe 1
                }
                game.isOnBattlefield("It of the Horrid Swarm") shouldBe true
            }
        }
    }
}
