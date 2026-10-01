package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Maze's Mantle (ONE #174) — {2}{G} Enchantment — Aura
 *   Flash. Enchant creature.
 *   When this Aura enters, if enchanted creature has toxic, that creature gains hexproof until
 *   end of turn.
 *   Enchanted creature gets +2/+2.
 */
class MazesMantleScenarioTest : ScenarioTestBase() {

    init {
        test("on a creature with toxic: +2/+2 and hexproof until end of turn") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Blightbelly Rat")
                .withCardInHand(1, "Maze's Mantle")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val rat = game.findPermanent("Blightbelly Rat")!!
            game.castSpell(1, "Maze's Mantle", rat).error shouldBe null
            game.resolveStack()
            game.resolveStack()

            game.isOnBattlefield("Maze's Mantle") shouldBe true
            game.state.projectedState.getPower(rat) shouldBe 4
            game.state.projectedState.getToughness(rat) shouldBe 4
            game.state.projectedState.hasKeyword(rat, Keyword.HEXPROOF) shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.projectedState.hasKeyword(rat, Keyword.HEXPROOF) shouldBe false
            game.state.projectedState.getPower(rat) shouldBe 4
        }

        test("on a creature without toxic: +2/+2 but no hexproof") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Maze's Mantle")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Maze's Mantle", bears).error shouldBe null
            game.resolveStack()
            game.resolveStack()

            game.isOnBattlefield("Maze's Mantle") shouldBe true
            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
            game.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe false
        }
    }
}
