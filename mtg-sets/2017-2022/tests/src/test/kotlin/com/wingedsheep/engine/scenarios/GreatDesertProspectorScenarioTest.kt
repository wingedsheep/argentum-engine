package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Great Desert Prospector (BRO #7) — {4}{W} 3/2. "When this creature enters, create a tapped
 * Powerstone token for each other creature you control."
 *
 * First card to create a resolution-counted number of Powerstones: the count excludes the
 * Prospector itself and the opponent's creatures, and every token enters tapped.
 */
class GreatDesertProspectorScenarioTest : ScenarioTestBase() {

    init {
        test("creates one tapped Powerstone per other creature you control") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Great Desert Prospector")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Great Desert Prospector").error shouldBe null
            game.resolveStack()
            game.resolveStack()

            val powerstones = game.findPermanents("Powerstone")
            powerstones shouldHaveSize 2
            powerstones.forEach { id ->
                val entity = game.state.getEntity(id)!!
                entity.has<TokenComponent>() shouldBe true
                entity.has<TappedComponent>() shouldBe true
                game.state.projectedState.hasType(id, "ARTIFACT") shouldBe true
                game.state.projectedState.getController(id) shouldBe game.player1Id
            }
        }

        test("creates nothing when it is your only creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Great Desert Prospector")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Great Desert Prospector").error shouldBe null
            game.resolveStack()
            game.resolveStack()

            game.findPermanent("Great Desert Prospector") shouldNotBe null
            game.findPermanents("Powerstone").shouldBeEmpty()
        }
    }
}
