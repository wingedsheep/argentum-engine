package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Molten Note. */
class MoltenNoteScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<PreparedSpellCopyComponent>() != null
        }
    }

    init {
        // -------------------------------------------------------------------
        // Molten Note
        // -------------------------------------------------------------------
        test("Molten Note deals damage equal to total mana spent and untaps your creatures") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Molten Note")
                .withCardOnBattlefield(1, "Grizzly Bears", tapped = true) // 2/2 tapped — should untap
                .withCardOnBattlefield(2, "War Behemoth") // 3/6 target — survives 5 damage
                .withLandsOnBattlefield(1, "Mountain", 4) // R + 3 generic
                .withLandsOnBattlefield(1, "Plains", 1) // W
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            val behemoth = game.findPermanent("War Behemoth")!!

            withClue("Grizzly Bears starts tapped") {
                game.state.getEntity(bears)?.get<TappedComponent>() shouldNotBe null
            }

            // X = 3 → {3}{R}{W} → 5 mana spent.
            game.castXSpell(1, "Molten Note", xValue = 3, targetId = behemoth).error shouldBe null
            game.resolveStack()

            withClue("damage equals the 5 mana spent to cast Molten Note") {
                game.state.getEntity(behemoth)?.get<DamageComponent>()?.amount shouldBe 5
            }
            withClue("all creatures you control untap") {
                game.state.getEntity(bears)?.get<TappedComponent>() shouldBe null
            }
        }
    }
}
