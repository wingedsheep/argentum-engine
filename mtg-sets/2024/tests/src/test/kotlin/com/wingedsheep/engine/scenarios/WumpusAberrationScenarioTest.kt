package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class WumpusAberrationScenarioTest : ScenarioTestBase() {
    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Wumpus Aberration")
        .withCardInHand(1, "Counterspell")
        .withCardInHand(2, "Grizzly Bears")
        .withCardInHand(2, "Forest")
        .withLandsOnBattlefield(1, "Forest", 4)
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

    init {
        test("colored-only payment lets the targeted opponent put a creature into play before Wumpus resolves") {
            val game = board()
            game.castSpell(1, "Wumpus Aberration").error shouldBe null
            game.state.stack.size shouldBe 2
            game.resolveStack()
            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            decision.playerId shouldBe game.player2Id
            decision.options shouldBe game.findCardsInHand(2, "Grizzly Bears")
            decision.minSelections shouldBe 0
            decision.maxSelections shouldBe 1
            game.selectCards(game.findCardsInHand(2, "Grizzly Bears")).error shouldBe null
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Wumpus Aberration") shouldBe false
            game.resolveStack()
            game.isOnBattlefield("Wumpus Aberration") shouldBe true
        }
        test("opponent may decline even when a creature is available") {
            val game = board()
            game.castSpell(1, "Wumpus Aberration").error shouldBe null
            game.resolveStack()
            game.skipSelection().error shouldBe null
            game.resolveStack()
            game.findCardsInHand(2, "Grizzly Bears").size shouldBe 1
            game.isOnBattlefield("Wumpus Aberration") shouldBe true
        }
        test("spending colorless mana prevents the trigger and target prompt") {
            // Exactly four mana sources ensure the solver spends colorless mana.
            val game = scenario().withPlayers("Player1", "Player2")
                .withCardInHand(1, "Wumpus Aberration")
                .withLandsOnBattlefield(1, "Forest", 3)
                .withLandsOnBattlefield(1, "Snow-Covered Wastes", 1)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, "Wumpus Aberration").error shouldBe null
            game.getPendingDecision() shouldBe null
            game.state.stack.size shouldBe 1
            game.resolveStack()
            game.isOnBattlefield("Wumpus Aberration") shouldBe true
        }
        test("countering Wumpus does not stop its unpaid-colorless trigger") {
            val game = board()
            game.castSpell(1, "Wumpus Aberration").error shouldBe null
            game.castSpellTargetingStackSpell(1, "Counterspell", "Wumpus Aberration").error shouldBe null
            game.resolveStack()
            game.selectCards(game.findCardsInHand(2, "Grizzly Bears")).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Wumpus Aberration") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
