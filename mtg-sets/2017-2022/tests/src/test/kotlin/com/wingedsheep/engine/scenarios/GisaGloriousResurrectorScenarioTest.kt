package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Gisa, Glorious Resurrector (Innistrad: Midnight Hunt #103).
 *
 * If a creature an opponent controls would die, exile it instead.
 * At the beginning of your upkeep, put all creature cards exiled with Gisa onto the battlefield
 * under your control. They gain decayed.
 */
class GisaGloriousResurrectorScenarioTest : ScenarioTestBase() {

    private fun gisaBoard(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Gisa, Glorious Resurrector")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInHand(1, "Lightning Bolt")
            .withCardInHand(1, "Lightning Bolt")
            .withLandsOnBattlefield(1, "Mountain", 2)
            .apply { repeat(4) { withCardInLibrary(1, "Swamp"); withCardInLibrary(2, "Swamp") } }
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        return game
    }

    init {
        test("an opponent's dying creature is exiled, then returns under your control with decayed at your upkeep") {
            val game = gisaBoard()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Lightning Bolt", giant).error shouldBe null
            game.resolveStack()

            withClue("the 3/3 would have died — Gisa exiles it instead") {
                game.isOnBattlefield("Hill Giant") shouldBe false
                game.isInGraveyard(2, "Hill Giant") shouldBe false
                game.isInExile(2, "Hill Giant") shouldBe true
            }

            // Opponent's turn: Gisa's upkeep trigger is "your upkeep" only, so nothing comes back yet.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("still the opponent's turn") { game.state.activePlayerId shouldBe game.player2Id }
            game.isInExile(2, "Hill Giant") shouldBe true

            // Your upkeep: the exiled creature card comes back under your control with decayed.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            game.resolveStack()

            val returned = game.findPermanent("Hill Giant")
                ?: error("Hill Giant should have returned to the battlefield")
            game.isInExile(2, "Hill Giant") shouldBe false
            withClue("it enters under Gisa's controller, not its owner") {
                game.state.getEntity(returned)?.get<ControllerComponent>()?.playerId shouldBe game.player1Id
            }
            withClue("it gains the real decayed keyword") {
                game.state.projectedState.hasKeyword(returned, Keyword.DECAYED) shouldBe true
            }
        }

        test("your own creatures still die normally and are not returned") {
            val game = gisaBoard()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInExile(1, "Grizzly Bears") shouldBe false
        }
    }
}
