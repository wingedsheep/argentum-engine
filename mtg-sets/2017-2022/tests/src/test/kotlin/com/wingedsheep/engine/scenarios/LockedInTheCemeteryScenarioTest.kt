package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Locked in the Cemetery (MID #60) — when it enters, if you have five or more cards in your
 * graveyard, tap enchanted creature; enchanted creature doesn't untap during its controller's
 * untap step.
 */
class LockedInTheCemeteryScenarioTest : ScenarioTestBase() {

    private fun game(graveyardCards: Int): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Locked in the Cemetery")
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(graveyardCards) { builder.withCardInGraveyard(1, "Island") }
        return builder.build()
    }

    init {
        test("taps the enchanted creature with five cards in your graveyard") {
            val game = game(graveyardCards = 5)
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Locked in the Cemetery", bears).error shouldBe null
            game.resolveStack()

            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            game.state.projectedState.hasKeyword(bears, AbilityFlag.DOESNT_UNTAP) shouldBe true
        }

        test("doesn't tap with four cards in your graveyard, but still locks the creature") {
            val game = game(graveyardCards = 4)
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Locked in the Cemetery", bears).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Locked in the Cemetery") shouldBe true
            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
            game.state.projectedState.hasKeyword(bears, AbilityFlag.DOESNT_UNTAP) shouldBe true
        }
    }
}
