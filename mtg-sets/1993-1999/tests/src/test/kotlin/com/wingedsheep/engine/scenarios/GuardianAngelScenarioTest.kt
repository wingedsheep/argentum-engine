package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe

class GuardianAngelScenarioTest : ScenarioTestBase() {
    init {
        fun board() = scenario().withPlayers()
            .withCardInHand(1, "Guardian Angel").withLandsOnBattlefield(1, "Plains", 6)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardInHand(2, "Lightning Bolt").withLandsOnBattlefield(2, "Mountain", 1)
            .withCardInLibrary(1, "Plains").withCardInLibrary(2, "Mountain")
            .withActivePlayer(1).build()
        fun cast(game: TestGame, target: ChosenTarget, x: Int) {
            game.execute(CastSpell(game.player1Id, game.findCardsInHand(1, "Guardian Angel").single(), listOf(target), xValue = x)).error shouldBe null
            game.resolveStack()
        }
        test("initial X shield and repeated special actions accumulate on the chosen player") {
            val game = board()
            cast(game, ChosenTarget.Player(game.player1Id), 1)
            val id = game.state.playerActionPermissions.single().id
            repeat(2) { game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null }
            game.state.stack shouldBe emptyList()
            game.state = game.state.withPriority(game.player2Id)
            game.castSpellTargetingPlayer(2, "Lightning Bolt", 1).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
            game.isInGraveyard(1, "Guardian Angel") shouldBe true
        }
        test("X zero still grants a usable permission for a creature") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            cast(game, ChosenTarget.Permanent(bears), 0)
            val id = game.state.playerActionPermissions.single().id
            repeat(2) { game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null }
            game.state = game.state.withPriority(game.player2Id)
            game.castSpell(2, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
        test("target that leaves and reenters is not the captured creature") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            cast(game, ChosenTarget.Permanent(bears), 0)
            val id = game.state.playerActionPermissions.single().id
            game.state = zones.moveToZone(game.state, bears, Zone.EXILE).state
            game.state = zones.moveToZone(game.state, bears, Zone.BATTLEFIELD).state
            repeat(2) { game.execute(TakePlayerAction(game.player1Id, id)).error shouldBe null }
            game.state = game.state.withPriority(game.player2Id)
            game.castSpell(2, "Lightning Bolt", bears).error shouldBe null
            game.resolveStack()
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
        }
        test("illegal target at resolution grants no permission") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            game.execute(CastSpell(game.player1Id, game.findCardsInHand(1, "Guardian Angel").single(), listOf(ChosenTarget.Permanent(bears)), xValue = 1)).error shouldBe null
            game.state = zones.moveToZone(game.state, bears, Zone.GRAVEYARD).state
            game.resolveStack()
            game.state.playerActionPermissions shouldBe emptyList()
        }
        test("permission expires at cleanup") {
            val game = board()
            cast(game, ChosenTarget.Player(game.player1Id), 0)
            game.passUntilPhase(com.wingedsheep.sdk.core.Phase.BEGINNING, com.wingedsheep.sdk.core.Step.UPKEEP)
            game.state.playerActionPermissions shouldBe emptyList()
        }
    }
}
