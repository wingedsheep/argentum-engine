package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Encase in Ice (DTK #54) — {1}{U} Enchantment — Aura.
 *
 *   Flash
 *   Enchant red or green creature
 *   When this Aura enters, tap enchanted creature.
 *   Enchanted creature doesn't untap during its controller's untap step.
 *
 * Pins the colour-gated enchant restriction: it can only target a red or green creature, and it is put
 * into the graveyard once the enchanted creature stops being red or green.
 */
class EncaseInIceScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(id: EntityId): Boolean = state.getEntity(id)!!.has<TappedComponent>()

    init {
        context("Encase in Ice") {

            test("flashed in on the opponent's turn, it taps a green creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Encase in Ice")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.castSpell(1, "Encase in Ice", bears)
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.isOnBattlefield("Encase in Ice") shouldBe true
                withClue("the ETB tapped the enchanted creature") { game.isTapped(bears) shouldBe true }
            }

            test("can't enchant a creature that is neither red nor green") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Encase in Ice")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lions = game.findPermanent("Savannah Lions")!!

                val cast = game.castSpell(1, "Encase in Ice", lions)
                withClue("a white creature is not a legal target") { cast.error shouldNotBe null }
            }

            test("is put into the graveyard when the enchanted creature stops being red or green") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Encase in Ice")
                    .withCardInHand(1, "Thoughtlace")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Raging Goblin")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goblin = game.findPermanent("Raging Goblin")!!

                val cast = game.castSpell(1, "Encase in Ice", goblin)
                withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()
                game.isOnBattlefield("Encase in Ice") shouldBe true

                val lace = game.castSpell(1, "Thoughtlace", goblin)
                withClue("Thoughtlace should succeed: ${lace.error}") { lace.error shouldBe null }
                game.resolveStack()

                withClue("the now-blue creature can't be enchanted by Encase in Ice") {
                    game.isOnBattlefield("Encase in Ice") shouldBe false
                    game.isInGraveyard(1, "Encase in Ice") shouldBe true
                }
            }
        }
    }
}
