package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Solid Footing (IKO #31) — "Enchanted creature gets +1/+1. As long as enchanted creature has
 * vigilance, it assigns combat damage equal to its toughness rather than its power."
 *
 * The vigilance gate is a battlefield-scoped filter (`attachedToBySource` + vigilance), so the
 * engine has to evaluate it against the enchanted creature at damage assignment.
 */
class SolidFootingScenarioTest : ScenarioTestBase() {

    private fun attackWith(creature: String): Int {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, creature)
            .withCardAttachedTo(1, "Solid Footing", creature)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()

        game.declareAttackers(mapOf(creature to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareNoBlockers().error shouldBe null
        game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
        return game.getLifeTotal(2)
    }

    init {
        context("Solid Footing") {

            test("a creature with vigilance assigns damage equal to its boosted toughness") {
                // Yotian Soldier 1/4 vigilance -> 2/5, deals 5.
                attackWith("Yotian Soldier") shouldBe 15
            }

            test("a creature without vigilance still assigns damage equal to its power") {
                // Horned Turtle 1/4 -> 2/5, deals 2.
                attackWith("Horned Turtle") shouldBe 18
            }
        }
    }
}
