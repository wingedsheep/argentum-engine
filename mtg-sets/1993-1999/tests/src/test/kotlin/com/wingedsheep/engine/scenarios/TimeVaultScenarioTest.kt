package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.matchers.shouldBe

class TimeVaultScenarioTest : ScenarioTestBase() {
    init {
        fun TestGame.isTapped(id: com.wingedsheep.sdk.model.EntityId) =
            state.getEntity(id)?.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>() == true
        fun board(tapped: Boolean = true, ownerTurn: Boolean = false) = scenario()
            .withPlayers("Keeper", "Opponent")
            .withCardOnBattlefield(1, "Time Vault", tapped = tapped)
            .withCardInLibrary(1, "Island").withCardInLibrary(1, "Island").withCardInLibrary(2, "Forest")
            .withActivePlayer(if (ownerTurn) 1 else 2).withPriorityPlayer(if (ownerTurn) 1 else 2)
            .inPhase(Phase.ENDING, Step.END).build()
        fun boundary(game: TestGame) {
            game.passPriority().error shouldBe null
            game.passPriority().error shouldBe null
        }
        fun choose(game: TestGame, accept: Boolean) {
            val decision = game.state.pendingDecision as ChooseOptionDecision
            decision.playerId shouldBe game.player1Id
            decision.options.first().contains("Untap this artifact") shouldBe true
            game.submitDecision(OptionChosenResponse(decision.id, if (accept) 0 else decision.options.lastIndex)).error shouldBe null
        }
        test("Time Vault enters tapped when cast") {
            val game = scenario().withPlayers("Keeper", "Opponent")
                .withCardInHand(1, "Time Vault").withLandsOnBattlefield(1, "Island", 2)
                .withCardInLibrary(1, "Island").withCardInLibrary(2, "Forest")
                .withActivePlayer(1).withPriorityPlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            game.castSpell(1, "Time Vault").error shouldBe null
            game.resolveStack()
            game.isTapped(game.findPermanent("Time Vault")!!) shouldBe true
        }
        test("declining turn skip leaves Time Vault tapped throughout your untap") {
            val game = board(); boundary(game); choose(game, false)
            game.state.activePlayerId shouldBe game.player1Id
            game.state.step shouldBe Step.UPKEEP
            game.isTapped(game.findPermanent("Time Vault")!!) shouldBe true
        }
        test("skipping your turn untaps Time Vault before the opponent takes their turn") {
            val game = board(); val n = game.state.turnNumber
            boundary(game); choose(game, true)
            game.state.activePlayerId shouldBe game.player2Id
            game.state.turnNumber shouldBe n + 1
            game.isTapped(game.findPermanent("Time Vault")!!) shouldBe false
        }
        test("an untapped Time Vault offers no skip") {
            val game = board(tapped = false); boundary(game)
            game.state.pendingDecision shouldBe null
            game.state.activePlayerId shouldBe game.player1Id
        }
        test("Time Vault does not offer to skip an opponent turn") {
            val game = board(ownerTurn = true); boundary(game)
            game.state.pendingDecision shouldBe null
            game.state.activePlayerId shouldBe game.player2Id
            game.isTapped(game.findPermanent("Time Vault")!!) shouldBe true
        }
        test("tap activation creates an extra turn and skipping that occurrence restores normal opponent turn") {
            val game = board(tapped = false, ownerTurn = true)
            val id = game.findPermanent("Time Vault")!!
            game.execute(ActivateAbility(game.player1Id, id, cardRegistry.getCard("Time Vault")!!.script.activatedAbilities.single().id)).error shouldBe null
            game.isTapped(id) shouldBe true
            game.resolveStack()
            boundary(game)
            choose(game, true)
            game.state.activePlayerId shouldBe game.player2Id
            game.isTapped(id) shouldBe false
        }
        test("tap activation extra turn can be taken while keeping the Vault tapped") {
            val game = board(tapped = false, ownerTurn = true); val id = game.findPermanent("Time Vault")!!
            game.execute(ActivateAbility(game.player1Id, id, cardRegistry.getCard("Time Vault")!!.script.activatedAbilities.single().id)).error shouldBe null
            game.resolveStack(); boundary(game); choose(game, false)
            game.state.activePlayerId shouldBe game.player1Id
            game.isTapped(id) shouldBe true
        }
        test("activating twice after another untap creates two extra turns without an additional turn debt") {
            val game = board(tapped = false, ownerTurn = true); val id = game.findPermanent("Time Vault")!!
            val ability = cardRegistry.getCard("Time Vault")!!.script.activatedAbilities.single().id
            game.execute(ActivateAbility(game.player1Id, id, ability)).error shouldBe null
            game.resolveStack()
            val untap = services.effectExecutorRegistry.execute(game.state, Effects.Untap(EffectTarget.Self),
                EffectContext(sourceId = id, controllerId = game.player1Id))
            untap.error shouldBe null; game.state = untap.state
            game.execute(ActivateAbility(game.player1Id, id, ability)).error shouldBe null
            game.resolveStack()
            repeat(2) {
                game.passUntilPhase(Phase.ENDING, Step.END)
                boundary(game); choose(game, false)
                game.state.activePlayerId shouldBe game.player1Id
            }
            game.passUntilPhase(Phase.ENDING, Step.END)
            boundary(game)
            game.state.activePlayerId shouldBe game.player2Id
            game.state.pendingDecision shouldBe null
            game.isTapped(id) shouldBe true
        }
        test("other untap effects work without skipping a turn") {
            val game = board(ownerTurn = true); val id = game.findPermanent("Time Vault")!!
            val result = services.effectExecutorRegistry.execute(game.state, Effects.Untap(EffectTarget.Self),
                EffectContext(sourceId = id, controllerId = game.player1Id))
            result.error shouldBe null; game.state = result.state
            game.isTapped(id) shouldBe false
            game.state.turnNumber shouldBe 1
            game.execute(ActivateAbility(game.player1Id, id, cardRegistry.getCard("Time Vault")!!.script.activatedAbilities.single().id)).error shouldBe null
        }
    }
}
