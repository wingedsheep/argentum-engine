package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Quartzwood Crasher: one trigger per player hit by tramplers you control, creating an X/X
 * trampling Dinosaur Beast where X is the total those tramplers dealt that player — damage from a
 * creature without trample doesn't count.
 */
class QuartzwoodCrasherScenarioTest : ScenarioTestBase() {
    init {
        test("two tramplers hitting the same player make one token sized by their combined damage") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Quartzwood Crasher")
                .withCardOnBattlefield(1, "Colossal Dreadmaw")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(
                mapOf("Quartzwood Crasher" to 2, "Colossal Dreadmaw" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            game.getLifeTotal(2) shouldBe 6

            val tokens = game.findPermanents("Dinosaur Beast Token")
            withClue("a single batched trigger, not one per trampler") {
                tokens shouldHaveSize 1
            }
            val token = tokens.single()
            val projected = game.state.projectedState
            withClue("X = 6 + 6 from the tramplers; Grizzly Bears' 2 is excluded") {
                projected.getPower(token) shouldBe 12
                projected.getToughness(token) shouldBe 12
            }
            projected.hasKeyword(token, Keyword.TRAMPLE) shouldBe true
        }
    }
}
