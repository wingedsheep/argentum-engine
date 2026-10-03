package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Winter's Rest (MH1 #78) — "Enchant creature / When this Aura enters, tap enchanted creature. /
 * As long as you control another snow permanent, enchanted creature doesn't untap during its
 * controller's untap step."
 *
 * The Aura is snow itself, so "another" means the lock needs a second snow permanent.
 */
class WintersRestScenarioTest : ScenarioTestBase() {

    private fun castAndAdvanceToBobsNextUpkeep(aliceLand: String): Pair<TestGame, com.wingedsheep.sdk.model.EntityId> {
        val game = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Winter's Rest")
            .withLandsOnBattlefield(1, aliceLand, 2)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Forest")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val bears = game.findPermanent("Grizzly Bears")!!

        game.castSpell(1, "Winter's Rest", bears).error shouldBe null
        game.resolveStack()
        withClue("the enters trigger tapped the creature") {
            game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
        }

        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        withClue("it's Bob's turn") { game.state.activePlayerId shouldBe game.player2Id }
        return game to bears
    }

    init {
        context("Winter's Rest") {

            test("with another snow permanent, the creature doesn't untap") {
                val (game, bears) = castAndAdvanceToBobsNextUpkeep("Snow-Covered Island")
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
            }

            test("with no other snow permanent, the creature untaps normally") {
                val (game, bears) = castAndAdvanceToBobsNextUpkeep("Island")
                game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe false
            }
        }
    }
}
