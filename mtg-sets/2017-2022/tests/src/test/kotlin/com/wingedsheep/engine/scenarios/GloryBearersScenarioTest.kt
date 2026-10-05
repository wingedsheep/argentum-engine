package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Glory Bearers (THB #17) — {3}{W} 3/4 Enchantment Creature — Human Cleric.
 *
 * "Whenever another creature you control attacks, it gets +0/+1 until end of turn."
 */
class GloryBearersScenarioTest : ScenarioTestBase() {

    init {
        test("each other attacking creature you control gets +0/+1 until end of turn; Bearers itself doesn't") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Glory Bearers")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Savannah Lions")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Glory Bearers" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("the other attacker gets +0/+1") {
                projected.getPower(bears) shouldBe 2
                projected.getToughness(bears) shouldBe 3
            }
            withClue("Glory Bearers doesn't pump itself") {
                projected.getToughness(game.findPermanent("Glory Bearers")!!) shouldBe 4
            }
            withClue("a non-attacking creature isn't pumped") {
                projected.getToughness(game.findPermanent("Savannah Lions")!!) shouldBe 1
            }

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("the pump ends at end of turn") {
                game.state.projectedState.getToughness(bears) shouldBe 2
            }
        }

        test("an opponent's attacking creature isn't pumped") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Glory Bearers")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getToughness(game.findPermanent("Grizzly Bears")!!) shouldBe 2
        }
    }
}
