package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Takeno, Samurai General (CHK #46) — "Bushido 2. Each other Samurai creature you control gets
 * +1/+1 for each point of bushido it has."
 */
class TakenoSamuraiGeneralScenarioTest : ScenarioTestBase() {

    init {
        context("Takeno, Samurai General") {

            test("each other Samurai you control gets +1/+1 per point of its own bushido") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Takeno, Samurai General")
                    .withCardOnBattlefield(1, "Numai Outcast")       // Human Samurai 1/1, bushido 2
                    .withCardOnBattlefield(1, "Kitsune Blademaster") // Fox Samurai, bushido 1
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Numai Outcast")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val projected = game.state.projectedState
                val takeno = game.findPermanent("Takeno, Samurai General")!!
                val (myOutcast, theirOutcast) = game.findPermanents("Numai Outcast")
                    .partition { projected.getController(it) == game.player1Id }
                    .let { (a, b) -> a.single() to b.single() }
                val blademaster = game.findPermanent("Kitsune Blademaster")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Numai Outcast: bushido 2 → 1/1 becomes 3/3") {
                    projected.getPower(myOutcast) shouldBe 3
                    projected.getToughness(myOutcast) shouldBe 3
                }
                withClue("Kitsune Blademaster: bushido 1 → +1/+1") {
                    projected.getPower(blademaster) shouldBe 3
                    projected.getToughness(blademaster) shouldBe 3
                }
                withClue("Takeno doesn't pump itself (\"other\")") {
                    projected.getPower(takeno) shouldBe 3
                }
                withClue("a non-Samurai gets nothing") {
                    projected.getPower(bears) shouldBe 2
                }
                withClue("an opponent's Samurai gets nothing") {
                    projected.getPower(theirOutcast) shouldBe 1
                }
            }

            test("bushido 2: Takeno gets +2/+2 when it becomes blocked") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Takeno, Samurai General")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val takeno = game.findPermanent("Takeno, Samurai General")!!
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Takeno, Samurai General" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Takeno, Samurai General"))).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(takeno) shouldBe 5
                game.state.projectedState.getToughness(takeno) shouldBe 5
            }
        }
    }
}
