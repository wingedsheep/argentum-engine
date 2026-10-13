package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Rundvelt Hordemaster — "Other Goblins you control get +1/+1. Whenever this creature or another
 * Goblin you control dies, exile the top card of your library. If it's a Goblin creature card,
 * you may cast that card until the end of your next turn."
 *
 * Covers: the lord skips Hordemaster itself; its own death fires the trigger (the source is gone
 * by the time it resolves) and a Goblin creature card exiled that way can be cast from exile; a
 * non-Goblin card is exiled with no permission.
 */
class RundveltHordemasterScenarioTest : ScenarioTestBase() {

    private fun board(topCard: String, extraGoblin: Boolean = false) = scenario()
        .withPlayers()
        .withCardOnBattlefield(1, "Rundvelt Hordemaster")
        .apply { if (extraGoblin) withCardOnBattlefield(1, "Raging Goblin") }
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInHand(1, "Shock")
        .withCardInLibrary(1, topCard)
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.playable(): Set<EntityId> =
        state.mayPlayPermissions.flatMap { it.cardIds }.toSet()

    init {
        test("other Goblins get +1/+1, Hordemaster does not") {
            val game = board("Island", extraGoblin = true)
            val hordemaster = game.findPermanent("Rundvelt Hordemaster")!!
            val goblin = game.findPermanent("Raging Goblin")!!
            game.state.projectedState.getPower(hordemaster) shouldBe 1
            game.state.projectedState.getPower(goblin) shouldBe 2
            game.state.projectedState.getToughness(goblin) shouldBe 2
        }

        test("its own death exiles a Goblin creature card that can then be cast") {
            val game = board("Raging Goblin")
            val hordemaster = game.findPermanent("Rundvelt Hordemaster")!!
            game.castSpell(1, "Shock", hordemaster).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Rundvelt Hordemaster") shouldBe true
            val exiled = game.state.getExile(game.player1Id).single()
            game.playable() shouldBe setOf(exiled)

            game.castSpellFromExile(1, "Raging Goblin").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Raging Goblin") shouldBe true
        }

        test("another Goblin dying exiles a non-Goblin card with no permission") {
            val game = board("Grizzly Bears", extraGoblin = true)
            val goblin = game.findPermanent("Raging Goblin")!!
            game.castSpell(1, "Shock", goblin).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Raging Goblin") shouldBe true
            game.state.getExile(game.player1Id).size shouldBe 1
            game.playable() shouldBe emptySet()
        }
    }
}
