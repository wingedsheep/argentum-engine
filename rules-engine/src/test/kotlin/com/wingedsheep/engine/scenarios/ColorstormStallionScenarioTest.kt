package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Colorstorm Stallion. */
class ColorstormStallionScenarioTest : ScenarioTestBase() {

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
        // Colorstorm Stallion
        // -------------------------------------------------------------------
        test("Colorstorm Stallion gets +1/+1 on a cheap instant/sorcery and makes no token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Colorstorm Stallion") // 3/3
                .withCardInHand(1, "Lightning Bolt") // {R}
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val stallion = game.findPermanent("Colorstorm Stallion")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", targetId = bears).error shouldBe null
            game.resolveStack()

            withClue("1 mana spent → +1/+1 only → 4/4") {
                projector.getProjectedPower(game.state, stallion) shouldBe 4
                projector.getProjectedToughness(game.state, stallion) shouldBe 4
            }
            withClue("no token copy below the 5-mana threshold") {
                game.findPermanents("Colorstorm Stallion").size shouldBe 1
            }
        }

        test("Colorstorm Stallion gets +1/+1 AND a token copy when 5+ mana is spent") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Colorstorm Stallion") // 3/3
                .withCardInHand(1, "Blaze") // {X}{R}
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val stallion = game.findPermanent("Colorstorm Stallion")!!
            val giant = game.findPermanent("Hill Giant")!!

            // Blaze X=4 → {4}{R} → 5 mana spent (boundary).
            game.castXSpell(1, "Blaze", xValue = 4, targetId = giant).error shouldBe null
            game.resolveStack()

            withClue("5 mana spent → +1/+1 → 4/4 on the original") {
                projector.getProjectedPower(game.state, stallion) shouldBe 4
            }
            withClue("a token copy of Colorstorm Stallion is created (2 now exist)") {
                game.findPermanents("Colorstorm Stallion").size shouldBe 2
            }
        }
    }
}
