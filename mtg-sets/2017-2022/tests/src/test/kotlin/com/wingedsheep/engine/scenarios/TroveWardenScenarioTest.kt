package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.mtg.sets.definitions.m10.cards.DoomBlade
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TroveWardenScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(DoomBlade)
        initMirrorMatch(Deck.of("Plains" to 30, "Swamp" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.drain() {
        var guard = 0
        while ((stackSize > 0 || pendingDecision != null) && guard++ < 30) {
            if (pendingDecision != null) autoResolveDecision() else bothPass()
        }
        stackSize shouldBe 0
        pendingDecision shouldBe null
    }

    fun GameTestDriver.landfall(player: EntityId, target: EntityId) {
        val land = putCardInHand(player, "Plains")
        (playLand(player, land).outcome is Outcome.Paused) shouldBe true
        val choice = pendingDecision as ChooseTargetsDecision
        (target in choice.legalTargets.values.flatten()) shouldBe true
        submitTargetSelection(player, listOf(target)).outcome shouldBe Outcome.Done
    }

    fun GameTestDriver.kill(player: EntityId, warden: EntityId) {
        giveMana(player, Color.BLACK, 1)
        giveColorlessMana(player, 1)
        castSpell(player, putCardInHand(player, "Doom Blade"), targets = listOf(warden)).outcome shouldBe Outcome.Done
    }

    test("landfall only targets your permanent cards costing at most three and death returns the pile") {
        val game = driver()
        val me = game.activePlayer!!
        val opp = game.getOpponent(me)
        val warden = game.putCreatureOnBattlefield(me, "Trove Warden")
        val bear = game.putCardInGraveyard(me, "Grizzly Bears")
        val expensive = game.putCardInGraveyard(me, "Trove Warden")
        val instant = game.putCardInGraveyard(me, "Doom Blade")
        val theirs = game.putCardInGraveyard(opp, "Grizzly Bears")
        val land = game.putCardInHand(me, "Plains")
        (game.playLand(me, land).outcome is Outcome.Paused) shouldBe true
        val legal = (game.pendingDecision as ChooseTargetsDecision).legalTargets.values.flatten()
        (bear in legal) shouldBe true
        listOf(expensive, instant, theirs).none { it in legal } shouldBe true
        game.submitTargetSelection(me, listOf(bear)).outcome shouldBe Outcome.Done
        game.drain()
        game.getExileCardNames(me).contains("Grizzly Bears") shouldBe true
        listOf(expensive, instant).all { it in game.getGraveyard(me) } shouldBe true
        (theirs in game.getGraveyard(opp)) shouldBe true
        game.kill(me, warden)
        game.drain()
        game.getPermanents(me).contains(bear) shouldBe true
        game.getExileCardNames(me).contains("Grizzly Bears") shouldBe false
    }

    test("landfall resolving after death exiles its card permanently") {
        val game = driver()
        val me = game.activePlayer!!
        val warden = game.putCreatureOnBattlefield(me, "Trove Warden")
        val bear = game.putCardInGraveyard(me, "Grizzly Bears")
        game.landfall(me, bear)
        game.kill(me, warden)
        game.drain()
        game.getGraveyard(me).contains(warden) shouldBe true
        game.getExileCardNames(me).contains("Grizzly Bears") shouldBe true
        game.getPermanents(me).contains(bear) shouldBe false
    }

    test("blinking the Warden abandons the old pile and its later death cannot return it") {
        val game = driver()
        val me = game.activePlayer!!
        val warden = game.putCreatureOnBattlefield(me, "Trove Warden")
        val bear = game.putCardInGraveyard(me, "Grizzly Bears")
        game.landfall(me, bear)
        game.drain()
        game.giveMana(me, Color.WHITE, 1)
        game.castSpell(me, game.putCardInHand(me, "Cloudshift"), targets = listOf(warden)).outcome shouldBe Outcome.Done
        game.drain()
        game.getExileCardNames(me).contains("Grizzly Bears") shouldBe true
        game.kill(me, warden)
        game.drain()
        game.getExileCardNames(me).contains("Grizzly Bears") shouldBe true
    }
    test("a stolen Warden returns multiple linked permanents including a land to their owner") {
        val game = driver()
        val me = game.activePlayer!!
        val opp = game.getOpponent(me)
        val warden = game.putCreatureOnBattlefield(me, "Trove Warden")
        val bear = game.putCardInGraveyard(me, "Grizzly Bears")
        game.landfall(me, bear)
        game.drain()
        val plains = game.putCardInGraveyard(me, "Plains")
        // A spell putting a land onto the battlefield also triggers landfall.
        game.giveMana(me, Color.GREEN, 1)
        game.giveColorlessMana(me, 1)
        game.castSpell(me, game.putCardInHand(me, "Rampant Growth")).outcome shouldBe Outcome.Done
        game.bothPass()
        val search = game.pendingDecision as SelectCardsDecision
        game.submitCardSelection(me, listOf(search.options.first()))
        game.submitTargetSelection(me, listOf(plains))
        game.drain()
        game.getExileCardNames(me).contains("Plains") shouldBe true
        game.replaceState(game.state.updateEntity(warden) { it.with(ControllerComponent(opp)) })
        game.kill(me, warden)
        game.drain()
        game.getController(bear) shouldBe me
        game.getController(plains) shouldBe me
        game.getPermanents(me).containsAll(listOf(bear, plains)) shouldBe true
    }

})
