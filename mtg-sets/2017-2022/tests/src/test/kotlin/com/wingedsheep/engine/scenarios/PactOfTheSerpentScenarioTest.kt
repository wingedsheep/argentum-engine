package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Pact of the Serpent — "Choose a creature type. Target player draws X cards and loses X life,
 * where X is the number of creatures they control of the chosen type."
 *
 * The player is targeted on cast and the type chosen on resolution; X counts only the *target
 * player's* creatures of that type (changelings included), and the same X feeds both the draw and
 * the life loss.
 */
class PactOfTheSerpentScenarioTest : ScenarioTestBase() {
    init {
        test("target opponent draws and loses life for each creature of the chosen type they control") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Pact of the Serpent")
                .withLandsOnBattlefield(1, "Swamp", 3)
                // The caster's own Goblin must not count toward the target player's X.
                .withCardOnBattlefield(1, "Raging Goblin")
                .withCardOnBattlefield(2, "Raging Goblin")
                .withCardOnBattlefield(2, "Raging Goblin")
                .withCardOnBattlefield(2, "Changeling Berserker")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(1, "Swamp")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpellTargetingPlayer(1, "Pact of the Serpent", 2).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Goblin")))

            withClue("two Goblins plus a changeling: X = 3") {
                game.handSize(2) shouldBe 3
                game.getLifeTotal(2) shouldBe 17
            }
            withClue("the caster is unaffected") {
                game.handSize(1) shouldBe 0
                game.getLifeTotal(1) shouldBe 20
            }
        }

        test("targeting yourself counts your own creatures of the chosen type") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Pact of the Serpent")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(1, "Raging Goblin")
                .withCardOnBattlefield(2, "Raging Goblin")
                .withCardOnBattlefield(2, "Raging Goblin")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpellTargetingPlayer(1, "Pact of the Serpent", 1).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(decision.id, decision.options.indexOf("Goblin")))

            game.handSize(1) shouldBe 1
            game.getLifeTotal(1) shouldBe 19
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
