package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class PirateShipScenarioTest : ScenarioTestBase() {
    init {
        val services = EngineServices(cardRegistry)
        fun board(ownIsland: Boolean = true, enemyIsland: Boolean = true) = scenario().withPlayers()
            .withCardOnBattlefield(1, "Pirate Ship", summoningSickness = false)
            .withLandsOnBattlefield(1, if (ownIsland) "Island" else "Mountain", 1)
            .withLandsOnBattlefield(2, if (enemyIsland) "Island" else "Mountain", 1)
            .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island").withCardInLibrary(2, "Island")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        fun fire(game: TestGame) {
            val polled = services.stateTriggerPoller.poll(game.state)
            polled.pendingTriggers.size shouldBe 1
            game.state = game.state.copy(pendingTriggers = polled.pendingTriggers)
            game.passPriority().error shouldBe null
            game.state.stack.size shouldBe 1
        }

        test("survives while its controller controls an Island") {
            val game = board()
            services.stateTriggerPoller.poll(game.state).pendingTriggers.size shouldBe 0
            game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            game.isOnBattlefield("Pirate Ship") shouldBe true
        }

        test("an opponent's Island does not prevent its sacrifice trigger") {
            val game = board(ownIsland = false)
            fire(game)
            game.isOnBattlefield("Pirate Ship") shouldBe true
            game.resolveStack()
            game.isInGraveyard(1, "Pirate Ship") shouldBe true
        }

        test("regaining an Island in response does not undo an already triggered sacrifice") {
            val game = board()
            val island = game.findPermanents("Island").first { game.state.projectedState.getController(it) == game.player1Id }!!
            game.state = game.zones.moveToZone(game.state, island, Zone.GRAVEYARD).state
            fire(game)
            game.state = game.zones.moveToZone(game.state, island, Zone.BATTLEFIELD).state
            game.resolveStack()
            game.isInGraveyard(1, "Pirate Ship") shouldBe true
        }

        test("countering its sacrifice trigger makes it trigger again while no Island is controlled") {
            val game = board(ownIsland = false)
            fire(game)
            val original = game.state.stack.single()
            game.state = services.stackResolver.counterAbility(game.state, original).state
            fire(game)
            game.state.stack.single() shouldNotBe original
            game.resolveStack()
            game.isInGraveyard(1, "Pirate Ship") shouldBe true
        }

        test("can attack an opponent who controls an Island") {
            val game = board()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Pirate Ship" to 2)).error shouldBe null
        }

        test("cannot attack an opponent without an Island") {
            val game = board(enemyIsland = false)
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Pirate Ship" to 2)).error shouldNotBe null
        }

        test("tap ability deals one damage to a player") {
            val game = board()
            val ship = game.findPermanent("Pirate Ship")!!
            game.execute(ActivateAbility(game.player1Id, ship,
                cardRegistry.getCard("Pirate Ship")!!.script.activatedAbilities.single().id,
                listOf(ChosenTarget.Player(game.player2Id)))).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 19
            game.state.getEntity(ship)!!.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() shouldBe true
        }

        test("tap ability works on creatures and deals damage after the Ship leaves") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Pirate Ship", summoningSickness = false)
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val ship = game.findPermanent("Pirate Ship")!!
            val victim = game.findPermanent("Llanowar Elves")!!
            game.execute(ActivateAbility(game.player1Id, ship,
                cardRegistry.getCard("Pirate Ship")!!.script.activatedAbilities.single().id,
                listOf(ChosenTarget.Permanent(victim)))).error shouldBe null
            game.state = game.zones.moveToZone(game.state, ship, Zone.GRAVEYARD).state
            game.resolveStack()
            game.isInGraveyard(2, "Llanowar Elves") shouldBe true
        }
    }
}
