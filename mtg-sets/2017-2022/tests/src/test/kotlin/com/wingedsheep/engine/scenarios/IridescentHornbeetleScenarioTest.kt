package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Iridescent Hornbeetle {4}{G} — 3/4 Insect. "At the beginning of your end step, create a 1/1 green
 * Insect creature token for each +1/+1 counter you've put on creatures under your control this turn."
 *
 * Rulings: counters put before the Hornbeetle was on the battlefield count, and so do the counters a
 * creature enters with. Counters you put on an opponent's creature don't.
 */
class IridescentHornbeetleScenarioTest : ScenarioTestBase() {

    private fun TestGame.cast(name: String, target: String? = null) {
        val targetId = target?.let { findPermanent(it) ?: error("no $it") }
        withClue("casting $name") { castSpell(1, name, targetId).error shouldBe null }
        resolveStack()
    }

    private fun TestGame.insectTokens() = findPermanents("Insect Token").size

    private fun TestGame.toEndStep() {
        passUntilPhase(Phase.ENDING, Step.END)
        resolveStack()
    }

    init {
        test("one Insect per +1/+1 counter put on your creatures, entering-with counters included") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Iridescent Hornbeetle")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withCardInHand(1, "Battlegrowth")
                .withCardInHand(1, "Battlegrowth")
                .withCardInHand(1, "Battlegrowth")
                .withCardInHand(1, "Spike Drone")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cast("Battlegrowth", "Grizzly Bears")
            game.cast("Battlegrowth", "Grizzly Bears")
            game.cast("Spike Drone")
            withClue("a counter on the opponent's creature isn't on a creature you control") {
                game.cast("Battlegrowth", "Hill Giant")
            }
            game.insectTokens() shouldBe 0

            game.toEndStep()

            withClue("two Battlegrowths on the Bears plus Spike Drone's entering counter") {
                game.insectTokens() shouldBe 3
            }
        }

        test("counters put before the Hornbeetle arrived still count") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInHand(1, "Battlegrowth")
                .withCardInHand(1, "Iridescent Hornbeetle")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.cast("Battlegrowth", "Grizzly Bears")
            game.cast("Iridescent Hornbeetle")

            game.toEndStep()

            game.insectTokens() shouldBe 1
        }

        test("no counters this turn, no Insects") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Iridescent Hornbeetle")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.toEndStep()

            game.insectTokens() shouldBe 0
        }
    }
}
