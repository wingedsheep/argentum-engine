package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Holo-Engineer {2}{G}{G} — Creature — Human Artificer 3/3
 *   When this creature enters, create a 3/3 colorless Illusion artifact creature token.
 *   If one or more tokens would be created under your control, that many 3/3 colorless Illusion
 *   artifact creature tokens are created instead.
 *
 * Pins the enters trigger (its own Illusion isn't replaced into a loop), a Treasure becoming an
 * Illusion, and an opponent's tokens being left alone.
 */
class HoloEngineerScenarioTest : ScenarioTestBase() {

    private fun TestGame.tokensOf(playerId: EntityId): List<EntityId> = state.getBattlefield().filter { id ->
        val c = state.getEntity(id) ?: return@filter false
        c.has<TokenComponent>() && c.get<ControllerComponent>()?.playerId == playerId
    }

    private fun TestGame.isIllusion(id: EntityId): Boolean {
        val projected = state.projectedState
        return projected.hasSubtype(id, "Illusion") && projected.isCreature(id) &&
            projected.getPower(id) == 3 && projected.getToughness(id) == 3 &&
            projected.getTypes(id).contains("ARTIFACT")
    }

    init {
        test("entering creates one 3/3 Illusion artifact creature token") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Holo-Engineer")
                .withLandsOnBattlefield(1, "Forest", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Holo-Engineer").error shouldBe null
            game.resolveStack()

            val tokens = game.tokensOf(game.player1Id)
            withClue("exactly one Illusion — the replacement doesn't loop on its own output") {
                tokens.size shouldBe 1
            }
            game.isIllusion(tokens.single()) shouldBe true
        }

        test("a Treasure you would create is an Illusion instead") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Holo-Engineer")
                .withCardInHand(1, "Strike It Rich")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Strike It Rich").error shouldBe null
            game.resolveStack()

            game.findPermanent("Treasure") shouldBe null
            val tokens = game.tokensOf(game.player1Id)
            tokens.size shouldBe 1
            game.isIllusion(tokens.single()) shouldBe true
        }

        test("an opponent's tokens are not replaced") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Holo-Engineer")
                .withCardInHand(2, "Strike It Rich")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Strike It Rich").error shouldBe null
            game.resolveStack()

            game.tokensOf(game.player2Id).single() shouldBe game.findPermanent("Treasure")
        }
    }
}
