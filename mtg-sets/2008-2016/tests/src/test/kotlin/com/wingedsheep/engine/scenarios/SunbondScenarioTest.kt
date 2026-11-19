package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Sunbond — {3}{W} Aura: Enchanted creature has "Whenever you gain life, put that many
 * +1/+1 counters on this creature." The granted trigger belongs to the creature, so "you" is
 * the creature's controller.
 */
class SunbondScenarioTest : ScenarioTestBase() {

    init {
        test("gaining 7 life puts seven +1/+1 counters on the enchanted creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Sunbond", "Grizzly Bears")
                .withCardInHand(1, "Angel's Mercy")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Angel's Mercy").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 27
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 9
            game.state.projectedState.getToughness(bears) shouldBe 9
        }

        test("on an opponent's creature, only that creature's controller gaining life counts") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardAttachedTo(1, "Sunbond", "Grizzly Bears")
                .withCardInHand(1, "Angel's Mercy")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Angel's Mercy").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 27
            val bears = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getPower(bears) shouldBe 2
        }
    }
}
