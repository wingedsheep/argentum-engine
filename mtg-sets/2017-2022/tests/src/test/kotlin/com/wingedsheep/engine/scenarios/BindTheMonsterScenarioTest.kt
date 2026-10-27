package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Bind the Monster (KHM #48) — when it enters, tap enchanted creature; that creature deals damage
 * to the Aura's controller equal to its power. The creature is the damage source, so its lifelink
 * gains life for *its* controller.
 */
class BindTheMonsterScenarioTest : ScenarioTestBase() {

    private fun game(creature: String): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Bind the Monster")
        .withLandsOnBattlefield(1, "Island", 1)
        .withCardOnBattlefield(2, creature)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("taps the creature, locks it, and it deals damage equal to its power to you") {
            val game = game("Hill Giant")
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Bind the Monster", giant).error shouldBe null
            game.resolveStack()

            game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
            game.state.projectedState.hasKeyword(giant, AbilityFlag.DOESNT_UNTAP) shouldBe true
            game.getLifeTotal(1) shouldBe 17
            game.getLifeTotal(2) shouldBe 20
        }

        test("the enchanted creature is the source, so its lifelink gains life for its controller") {
            val game = game("Vampire Nighthawk")
            val nighthawk = game.findPermanent("Vampire Nighthawk")!!
            game.castSpell(1, "Bind the Monster", nighthawk).error shouldBe null
            game.resolveStack()

            game.state.getEntity(nighthawk)?.has<TappedComponent>() shouldBe true
            game.getLifeTotal(1) shouldBe 18
            game.getLifeTotal(2) shouldBe 22
        }
    }
}
