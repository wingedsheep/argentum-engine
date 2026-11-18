package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Oakheart Dryads (JOU #133) — {2}{G} Enchantment Creature — Nymph Dryad 2/3.
 *
 *   Constellation — Whenever this creature or another enchantment you control enters, target
 *   creature gets +1/+1 until end of turn.
 */
class OakheartDryadsScenarioTest : ScenarioTestBase() {
    init {
        test("its own entry pumps the targeted creature until end of turn") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Oakheart Dryads")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Oakheart Dryads").error shouldBe null
            game.resolveStack()

            withClue("constellation asks for a target") {
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
        }

        test("another enchantment entering under your control triggers it too") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Oakheart Dryads")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Glorious Anthem")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Glorious Anthem").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            withClue("2/2 Bears + Anthem's +1/+1 + constellation's +1/+1") {
                game.state.projectedState.getPower(bears) shouldBe 4
                game.state.projectedState.getToughness(bears) shouldBe 4
            }
        }
    }
}
