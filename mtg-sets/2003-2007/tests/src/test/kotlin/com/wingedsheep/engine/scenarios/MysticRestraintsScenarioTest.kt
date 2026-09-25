package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mystic Restraints (CHK #76) — "Flash / Enchant creature / When this Aura enters, tap enchanted
 * creature. / Enchanted creature doesn't untap during its controller's untap step."
 *
 * Cast on the opponent's turn (flash) to tap an untapped creature, then the creature stays tapped
 * through its controller's next untap step.
 */
class MysticRestraintsScenarioTest : ScenarioTestBase() {

    init {
        context("Mystic Restraints") {

            test("flashed in on the opponent's turn, it taps the creature and keeps it tapped") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Mystic Restraints")
                    .withLandsOnBattlefield(1, "Island", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Mystic Restraints", bears).error shouldBe null
                game.resolveStack()

                withClue("the Aura attached and its enters trigger tapped the creature") {
                    game.isOnBattlefield("Mystic Restraints") shouldBe true
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }

                // Through Alice's turn into Bob's next untap step and past it.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                withClue("it's Bob's turn again") { game.state.activePlayerId shouldBe game.player2Id }
                withClue("the creature didn't untap during its controller's untap step") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
