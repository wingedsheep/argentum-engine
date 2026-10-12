package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hydra's Growth (THB #172) — {2}{G} Enchantment — Aura.
 *
 * "When this Aura enters, put a +1/+1 counter on enchanted creature. At the beginning of your
 * upkeep, double the number of +1/+1 counters on enchanted creature."
 */
class HydrasGrowthScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    // Six turns pass in the first test: both players need cards to draw.
    private fun ScenarioBuilder.withLibraryCards(n: Int): ScenarioBuilder {
        var b = this
        repeat(n) { b = b.withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest") }
        return b
    }

    private fun TestGame.passToNextUpkeep() {
        passUntilPhase(Phase.ENDING, Step.END)
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
    }

    init {
        context("Hydra's Growth") {

            test("ETB puts one counter, then each upkeep doubles: 1 -> 2 -> 4 -> 8") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Hydra's Growth")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLibraryCards(10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Hydra's Growth", game.findPermanent("Grizzly Bears")).error shouldBe null
                game.resolveStack()
                withClue("ETB counter") { game.plusOnes("Grizzly Bears") shouldBe 1 }

                game.passToNextUpkeep() // Bob's
                game.passToNextUpkeep()
                withClue("first upkeep doubles 1 -> 2") { game.plusOnes("Grizzly Bears") shouldBe 2 }

                game.passToNextUpkeep()
                game.passToNextUpkeep()
                withClue("second upkeep doubles 2 -> 4") { game.plusOnes("Grizzly Bears") shouldBe 4 }

                game.passToNextUpkeep()
                game.passToNextUpkeep()
                withClue("third upkeep doubles 4 -> 8") { game.plusOnes("Grizzly Bears") shouldBe 8 }
            }

            test("doubles an existing stack of counters on the enchanted creature") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Hydra's Growth", "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state = game.state.updateEntity(bears) {
                    it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 3)))
                }

                game.passToNextUpkeep() // Alice's
                game.plusOnes("Grizzly Bears") shouldBe 6
            }
        }
    }
}
