package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Paradox Engine (AER #169) — {5} Legendary Artifact.
 *
 *   Whenever you cast a spell, untap all nonland permanents you control.
 *
 * Covers the group's edges: it untaps the Engine itself and your other nonland permanents, leaves
 * the lands you tapped to cast the spell tapped, ignores the opponent's permanents, and doesn't
 * trigger on an opponent's spell.
 */
class ParadoxEngineScenarioTest : ScenarioTestBase() {

    private fun TestGame.isTapped(id: EntityId): Boolean =
        state.getEntity(id)!!.has<TappedComponent>()

    private fun board(activePlayer: Int) = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Paradox Engine", tapped = true)
        .withCardOnBattlefield(1, "Hill Giant", tapped = true)
        .withCardOnBattlefield(2, "Hill Giant", tapped = true)
        .withCardInHand(activePlayer, "Grizzly Bears")
        .withLandsOnBattlefield(activePlayer, "Forest", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(activePlayer)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.permanentsOf(player: Int, name: String): List<EntityId> =
        state.getBattlefield(if (player == 1) player1Id else player2Id).filter {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    init {
        test("casting a spell untaps your nonland permanents, not your lands or the opponent's") {
            val game = board(activePlayer = 1)
            val engine = game.findPermanent("Paradox Engine")!!
            val myGiant = game.permanentsOf(1, "Hill Giant").single()
            val theirGiant = game.permanentsOf(2, "Hill Giant").single()

            game.castSpell(1, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.isTapped(engine) shouldBe false
            game.isTapped(myGiant) shouldBe false
            game.isTapped(theirGiant) shouldBe true
            game.permanentsOf(1, "Forest").forEach { game.isTapped(it) shouldBe true }
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("an opponent's spell does not trigger it") {
            val game = board(activePlayer = 2)
            val engine = game.findPermanent("Paradox Engine")!!
            val myGiant = game.permanentsOf(1, "Hill Giant").single()

            game.castSpell(2, "Grizzly Bears").error shouldBe null
            game.resolveStack()

            game.isTapped(engine) shouldBe true
            game.isTapped(myGiant) shouldBe true
        }
    }
}
