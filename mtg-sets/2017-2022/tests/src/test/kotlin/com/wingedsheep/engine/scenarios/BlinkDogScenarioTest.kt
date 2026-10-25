package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Blink Dog (Adventures in the Forgotten Realms #3).
 *
 * Double strike
 * Teleport — {3}{W}: This creature phases out.
 *
 * Proves the Teleport ability phases the Dog out in response to removal, so the removal spell
 * loses its target and the Dog survives.
 */
class BlinkDogScenarioTest : ScenarioTestBase() {

    init {
        test("Teleport in response to Lightning Bolt phases the Dog out and it survives") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Blink Dog")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(2)
                .withPriorityPlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val dog = game.findPermanent("Blink Dog")!!
            game.castSpell(2, "Lightning Bolt", dog).error shouldBe null
            game.passPriority()

            val teleport = cardRegistry.getCard("Blink Dog")!!.script.activatedAbilities[0].id
            game.execute(
                ActivateAbility(playerId = game.player1Id, sourceId = dog, abilityId = teleport)
            ).error shouldBe null
            game.resolveStack()

            withClue("the Dog is phased out, so the Bolt had no legal target") {
                (game.state.getEntity(dog)?.get<PhasedOutComponent>() != null) shouldBe true
                game.isInGraveyard(1, "Blink Dog") shouldBe false
            }
        }
    }
}
