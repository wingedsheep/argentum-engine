package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bfz.cards.ObNixilisReignited
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Ob Nixilis Reignited (BFZ #119): the −8 emblem belongs to the *targeted opponent* (2015-08-25
 * ruling), so every card either player draws makes that opponent — never Ob Nixilis's controller —
 * lose 2 life, even after Ob Nixilis has left the battlefield. The +1 draws and costs its
 * controller 1 life.
 */
class ObNixilisReignitedScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(ObNixilisReignited))
        it.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun putObNixilis(d: GameTestDriver, player: EntityId, loyalty: Int): EntityId =
        d.putPermanentOnBattlefield(player, "Ob Nixilis Reignited").also {
            d.addComponent(it, CountersComponent(mapOf(CounterType.LOYALTY to loyalty)))
        }
    fun GameTestDriver.drain() {
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
    }
    val abilities = ObNixilisReignited.script.activatedAbilities

    test("+1 draws a card and its controller loses 1 life") {
        val d = driver()
        val me = d.activePlayer!!
        val ob = putObNixilis(d, me, 5)
        val handBefore = d.getHandSize(me)

        d.submitSuccess(ActivateAbility(me, ob, abilities[0].id))
        d.drain()

        d.getHandSize(me) shouldBe handBefore + 1
        d.getLifeTotal(me) shouldBe 19
    }

    test("−8 emblem makes the targeted opponent lose 2 life whenever any player draws, after Ob Nixilis is gone") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val ob = putObNixilis(d, me, 8)

        d.submitSuccess(ActivateAbility(me, ob, abilities[2].id, targets = listOf(ChosenTarget.Player(opp))))
        d.drain()

        val emblem = d.state.globalGrantedTriggeredAbilities.single()
        emblem.controllerId shouldBe opp
        d.moveToGraveyard(ob)

        // The opponent's own draw: they lose 2.
        d.passPriorityUntil(Step.DRAW, opp)
        d.drain()
        d.getLifeTotal(opp) shouldBe 18
        d.getLifeTotal(me) shouldBe 20

        // Ob Nixilis's controller draws: the emblem's owner still loses 2, the drawer loses nothing.
        d.passPriorityUntil(Step.DRAW, me)
        d.drain()
        d.getLifeTotal(opp) shouldBe 16
        d.getLifeTotal(me) shouldBe 20
    }
})
