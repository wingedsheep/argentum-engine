package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m21.cards.ChandrasIncinerator
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Chandra's Incinerator (M21 #136) — {5}{R} Creature — Elemental 6/6.
 *
 *   "This spell costs {X} less to cast, where X is the total amount of noncombat damage dealt to
 *    your opponents this turn.
 *    Trample
 *    Whenever a source you control deals noncombat damage to an opponent, this creature deals that
 *    much damage to target creature or planeswalker that player controls."
 */
class ChandrasIncineratorScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + ChandrasIncinerator)
        initMirrorMatch(deck = Deck.of("Mountain" to 60), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveStack() {
        while (!isPaused && state.stack.isNotEmpty()) bothPass()
    }

    fun GameTestDriver.boltOpponent(): Unit {
        val me = activePlayer!!
        val bolt = putCardInHand(me, "Lightning Bolt")
        giveMana(me, Color.RED, 1)
        castSpell(me, bolt, listOf(getOpponent(me))).error shouldBe null
        resolveStack()
    }

    test("with no noncombat damage dealt this turn it costs the full {5}{R}") {
        val d = driver()
        val me = d.activePlayer!!
        val incinerator = d.putCardInHand(me, ChandrasIncinerator.name)
        d.giveMana(me, Color.RED, 3)
        d.castSpell(me, incinerator).error shouldNotBe null
    }

    test("noncombat damage dealt to an opponent reduces its generic cost by that much") {
        val d = driver()
        val me = d.activePlayer!!
        d.boltOpponent()
        d.getLifeTotal(d.getOpponent(me)) shouldBe 17

        // 3 noncombat damage: {5}{R} becomes {2}{R}.
        val incinerator = d.putCardInHand(me, ChandrasIncinerator.name)
        d.giveMana(me, Color.RED, 3)
        d.castSpell(me, incinerator).error shouldBe null
        d.resolveStack()
        d.getPermanents(me).map { d.getCardName(it) }.contains(ChandrasIncinerator.name) shouldBe true
    }

    test("the reduction never touches the {R}") {
        val d = driver()
        val me = d.activePlayer!!
        d.boltOpponent()
        d.boltOpponent()
        d.boltOpponent()

        // 9 noncombat damage still leaves {R} to pay.
        val incinerator = d.putCardInHand(me, ChandrasIncinerator.name)
        d.castSpell(me, incinerator).error shouldNotBe null
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, incinerator).error shouldBe null
    }

    test("noncombat damage dealt to an opponent who has since lost the game still counts") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + ChandrasIncinerator)
            initMultiplayer(List(3) { Deck.of("Mountain" to 60) }, startingLife = 3)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        val loser = d.state.turnOrder.last()
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(loser)).error shouldBe null
        // Three seats: pass around the table until the Bolt resolves and priority is back with us.
        while (d.state.stack.isNotEmpty()) d.passPriority(d.state.priorityPlayerId!!)
        d.state.getEntity(loser)?.has<PlayerLostComponent>() shouldBe true

        val incinerator = d.putCardInHand(me, ChandrasIncinerator.name)
        d.giveMana(me, Color.RED, 3)
        d.castSpell(me, incinerator).error shouldBe null
    }

    test("combat damage dealt to an opponent doesn't reduce the cost") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.removeSummoningSickness(bears)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(bears), opp).error shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.getLifeTotal(opp) shouldBe 18

        val incinerator = d.putCardInHand(me, ChandrasIncinerator.name)
        d.giveMana(me, Color.RED, 4)
        d.castSpell(me, incinerator).error shouldNotBe null
    }

    test("a source you control dealing noncombat damage to an opponent deals that much to a creature that player controls") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + ChandrasIncinerator)
            initMultiplayer(List(3) { Deck.of("Mountain" to 60) })
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        val (damaged, other) = d.state.turnOrder.filter { it != me }
        d.putCreatureOnBattlefield(me, ChandrasIncinerator.name)
        val myBears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        val damagedBears = d.putCreatureOnBattlefield(damaged, "Grizzly Bears")
        val otherBears = d.putCreatureOnBattlefield(other, "Grizzly Bears")

        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(damaged)).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.passPriority(d.state.priorityPlayerId!!)
        d.getLifeTotal(damaged) shouldBe 17

        // "That player controls": only the damaged opponent's creature is a legal target.
        val legal = (d.pendingDecision as ChooseTargetsDecision).legalTargets.values.flatten()
        legal.contains(damagedBears) shouldBe true
        legal.contains(otherBears) shouldBe false
        legal.contains(myBears) shouldBe false
        d.submitTargetSelection(me, listOf(damagedBears)).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.passPriority(d.state.priorityPlayerId!!)
        d.getGraveyard(damaged).contains(damagedBears) shouldBe true
    }

    test("noncombat damage to a creature doesn't trigger it") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putCreatureOnBattlefield(me, ChandrasIncinerator.name)
        val oppBears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED, 1)
        d.castSpell(me, bolt, listOf(oppBears)).error shouldBe null
        d.resolveStack()
        (d.pendingDecision is ChooseTargetsDecision) shouldBe false
        d.getLifeTotal(opp) shouldBe 20
    }
})
