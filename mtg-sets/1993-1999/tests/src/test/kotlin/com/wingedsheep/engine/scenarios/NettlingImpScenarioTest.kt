package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ControlHistory
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class NettlingImpScenarioTest : ScenarioTestBase() {
    init {
        fun board(tapped: Boolean = false) = scenario().withPlayers()
            .withCardOnBattlefield(1, "Nettling Imp")
            .withCardOnBattlefield(2, "Grizzly Bears", tapped = tapped)
            .withCardOnBattlefield(2, "Wall of Wood")
            .withCardInLibrary(1, "Swamp").withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Forest").withCardInLibrary(2, "Forest")
            .withActivePlayer(2).withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
        fun activate(game: TestGame, name: String = "Grizzly Bears") = game.execute(ActivateAbility(
            game.player1Id, game.findPermanent("Nettling Imp")!!,
            cardRegistry.getCard("Nettling Imp")!!.script.activatedAbilities.first().id,
            listOf(ChosenTarget.Permanent(game.findPermanent(name)!!))))

        test("a tapped creature that cannot attack is destroyed even when the Imp leaves") {
            val game = board(tapped = true)
            activate(game).error shouldBe null
            game.resolveStack()
            val imp = game.findPermanent("Nettling Imp")!!
            game.state = game.zones.moveToZone(game.state, imp, com.wingedsheep.sdk.core.Zone.GRAVEYARD).state
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.findPermanent("Wall of Wood") shouldNotBe null
        }

        test("the creature must attack if able and attacking spares it at end step") {
            val game = board()
            activate(game).error shouldBe null
            game.resolveStack()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.execute(DeclareAttackers(game.player2Id, emptyMap())).error shouldNotBe null
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }

        test("changing control after an attack does not erase the attack") {
            val game = board()
            activate(game).error shouldBe null
            game.resolveStack()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            val bear = game.findPermanent("Grizzly Bears")!!
            game.state = game.state.updateEntity(bear) { it.with(ControllerComponent(game.player1Id)) }
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }

        test("a projected control reversion invalidates target legality") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Nettling Imp")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(2, "Control Magic", "Grizzly Bears")
                .withActivePlayer(2).withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.state = ControlHistory.beginTurn(game.state)
            val bear = game.findPermanent("Grizzly Bears")!!
            game.state.projectedState.getController(bear) shouldBe game.player2Id
            activate(game).error shouldBe null
            val aura = game.findPermanent("Control Magic")!!
            game.state = game.zones.moveToZone(game.state, aura, com.wingedsheep.sdk.core.Zone.GRAVEYARD).state
            game.resolveStack()
            game.state.projectedState.getController(bear) shouldBe game.player1Id
            game.state.controlAtTurnStart!!.containsKey(bear) shouldBe false
            game.state.delayedTriggers.size shouldBe 0
        }

        test("Walls, new entrants, and regained creatures are illegal targets") {
            val wall = board()
            activate(wall, "Wall of Wood").error shouldNotBe null
            val entered = board()
            val bear = entered.findPermanent("Grizzly Bears")!!
            entered.state = entered.state.updateEntity(bear) { it.with(EnteredThisTurnComponent) }
            activate(entered).error shouldNotBe null
            val regained = board()
            val id = regained.findPermanent("Grizzly Bears")!!
            regained.state = ControlHistory.beginTurn(regained.state)
            regained.state = ControlHistory.record(regained.state.updateEntity(id) { it.with(ControllerComponent(regained.player1Id)) }, emptyList())
                .updateEntity(id) { it.with(ControllerComponent(regained.player2Id)) }
            activate(regained).error shouldNotBe null
        }

        test("target legality is rechecked after an interruption of control") {
            val game = board()
            activate(game).error shouldBe null
            val bear = game.findPermanent("Grizzly Bears")!!
            game.state = ControlHistory.record(game.state.updateEntity(bear) { it.with(ControllerComponent(game.player1Id)) }, emptyList())
                .updateEntity(bear) { it.with(ControllerComponent(game.player2Id)) }
            game.resolveStack()
            game.state.delayedTriggers.size shouldBe 0
        }

        test("activation is forbidden on your turn and after attackers were declared") {
            val own = board()
            own.state = own.state.copy(activePlayerId = own.player1Id)
            activate(own).error shouldNotBe null
            val late = board()
            late.state = late.state.copy(phase = Phase.POSTCOMBAT_MAIN, step = Step.POSTCOMBAT_MAIN)
            activate(late).error shouldNotBe null
        }

        test("blinked target is a new object and survives delayed destruction") {
            val game = board(tapped = true)
            activate(game).error shouldBe null
            game.resolveStack()
            val bear = game.findPermanent("Grizzly Bears")!!
            game.state = game.zones.moveToZone(game.state, bear, com.wingedsheep.sdk.core.Zone.EXILE).state
            game.state = game.zones.moveToZone(game.state, bear, com.wingedsheep.sdk.core.Zone.BATTLEFIELD).state
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()
            game.findPermanent("Grizzly Bears") shouldNotBe null
        }
    }
}
