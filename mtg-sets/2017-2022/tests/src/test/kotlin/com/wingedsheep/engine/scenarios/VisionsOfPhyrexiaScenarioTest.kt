package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.VisionsOfPhyrexia
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VisionsOfPhyrexiaScenarioTest : FunSpec({
    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all + PredefinedTokens.allTokens + VisionsOfPhyrexia)
        initMirrorMatch(Deck.of("Mountain" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.powerstones(player: EntityId) = state.getBattlefield(player).filter {
        state.getEntity(it)?.get<CardComponent>()?.name == "Powerstone"
    }
    fun GameTestDriver.grant(player: EntityId, card: EntityId) {
        replaceState(state.addMayPlayPermission(MayPlayPermission(
            EntityId.generate(), setOf(card), player, timestamp = state.timestamp
        )))
    }
    fun GameTestDriver.upkeepExile(name: String): EntityId {
        val me = activePlayer!!
        passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = getOpponent(me))
        putPermanentOnBattlefield(me, VisionsOfPhyrexia.name)
        val top = putCardOnTopOfLibrary(me, name)
        passPriorityUntil(Step.UPKEEP, activePlayer = me)
        stackSize shouldBe 1
        bothPass().error shouldBe null
        state.getZone(ZoneKey(me, Zone.EXILE)) shouldBe listOf(top)
        return top
    }
    fun GameTestDriver.resolveEnd(player: EntityId) {
        passPriorityUntil(Step.END, activePlayer = player)
        while (state.stack.isNotEmpty()) bothPass().error shouldBe null
    }

    test("upkeep land can be played only during main phase and suppresses the end trigger") {
        val d = setup(); val me = d.activePlayer!!
        val land = d.upkeepExile("Forest")
        d.legalActions(me).any { it.affordable && (it.action as? PlayLand)?.cardId == land } shouldBe false
        (d.playLand(me, land).error != null) shouldBe true
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.legalActions(me).any { it.affordable && (it.action as? PlayLand)?.cardId == land } shouldBe true
        d.playLand(me, land).error shouldBe null
        d.passPriorityUntil(Step.END)
        d.stackSize shouldBe 0
        d.powerstones(me).size shouldBe 0
    }

    test("upkeep spell follows normal timing and costs and suppresses the end trigger") {
        val d = setup(); val me = d.activePlayer!!
        val creature = d.upkeepExile("Grizzly Bears")
        d.giveMana(me, Color.GREEN, 2)
        d.legalActions(me).any { it.affordable && (it.action as? CastSpell)?.cardId == creature } shouldBe false
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        (d.castSpell(me, creature).error != null) shouldBe true
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, creature).error shouldBe null
        d.bothPass().error shouldBe null
        d.resolveEnd(me)
        d.powerstones(me).size shouldBe 0
    }

    test("hand plays do not suppress a tapped Powerstone and only your end step triggers") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, VisionsOfPhyrexia.name)
        d.playLand(me, d.putCardInHand(me, "Forest")).error shouldBe null
        d.giveMana(me, Color.RED)
        d.castSpell(me, d.putCardInHand(me, "Lightning Bolt"), listOf(opp)).error shouldBe null
        d.bothPass().error shouldBe null
        d.resolveEnd(me)
        d.powerstones(me).size shouldBe 1
        d.state.getEntity(d.powerstones(me).single())!!.has<TappedComponent>() shouldBe true
        d.resolveEnd(opp)
        d.powerstones(me).size shouldBe 1
    }

    test("any permitted exile land counts even if owned by the opponent and played before Visions enters") {
        val d = setup(); val me = d.activePlayer!!
        val land = d.putCardInExile(d.getOpponent(me), "Forest")
        d.grant(me, land)
        d.playLand(me, land).error shouldBe null
        d.putPermanentOnBattlefield(me, VisionsOfPhyrexia.name)
        d.passPriorityUntil(Step.END)
        d.stackSize shouldBe 0
    }

    test("an exile play by the opponent does not suppress your Powerstone") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, VisionsOfPhyrexia.name)
        val bolt = d.putCardInExile(opp, "Lightning Bolt")
        d.grant(opp, bolt)
        d.passPriority(me)
        d.giveMana(opp, Color.RED)
        d.castSpell(opp, bolt, listOf(me)).error shouldBe null
        d.bothPass().error shouldBe null
        d.resolveEnd(me)
        d.powerstones(me).size shouldBe 1
    }

    test("casting the exiled instant in response makes the intervening-if fail on resolution") {
        val d = setup(); val me = d.activePlayer!!
        val bolt = d.upkeepExile("Lightning Bolt")
        d.passPriorityUntil(Step.END)
        d.stackSize shouldBe 1
        d.giveMana(me, Color.RED)
        d.castSpell(me, bolt, listOf(d.getOpponent(me))).error shouldBe null
        d.bothPass().error shouldBe null
        d.bothPass().error shouldBe null
        d.powerstones(me).size shouldBe 0
    }

    test("waiting until after the Powerstone resolves lets you cast the exiled instant too") {
        val d = setup(); val me = d.activePlayer!!
        val bolt = d.upkeepExile("Lightning Bolt")
        d.resolveEnd(me)
        d.powerstones(me).size shouldBe 1
        d.giveMana(me, Color.RED)
        d.castSpell(me, bolt, listOf(d.getOpponent(me))).error shouldBe null
        d.bothPass().error shouldBe null
        d.powerstones(me).size shouldBe 1
    }

    test("unplayed exile permission expires and last turn's land play does not suppress this turn") {
        val d = setup(); val me = d.activePlayer!!
        val land = d.upkeepExile("Forest")
        val second = d.putCardInExile(me, "Forest")
        d.grant(me, second)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.playLand(me, second).error shouldBe null
        // The normal land limit still applies to the upkeep card.
        (d.playLand(me, land).error != null) shouldBe true
        d.resolveEnd(me)
        d.powerstones(me).size shouldBe 0
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = d.getOpponent(me))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = me)
        d.legalActions(me).any { it.affordable && (it.action as? PlayLand)?.cardId == land } shouldBe false
        (d.playLand(me, land).error != null) shouldBe true
        d.resolveEnd(me)
        d.powerstones(me).size shouldBe 1
    }

    test("a countered spell cast from exile still suppresses the token") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, VisionsOfPhyrexia.name)
        val bolt = d.putCardInExile(me, "Lightning Bolt")
        d.grant(me, bolt)
        d.giveMana(me, Color.RED)
        d.castSpell(me, bolt, listOf(opp)).error shouldBe null
        d.passPriority(me)
        d.giveMana(opp, Color.BLUE, 2)
        d.castSpellWithTargets(opp, d.putCardInHand(opp, "Counterspell"), listOf(ChosenTarget.Spell(d.state.stack.single()))).error shouldBe null
        d.bothPass().error shouldBe null
        d.getLifeTotal(opp) shouldBe 20
        d.resolveEnd(me)
        d.powerstones(me).size shouldBe 0
    }

    test("the end-step trigger still creates its token after Visions leaves") {
        val d = setup(); val me = d.activePlayer!!
        val source = d.putPermanentOnBattlefield(me, VisionsOfPhyrexia.name)
        d.passPriorityUntil(Step.END)
        d.stackSize shouldBe 1
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, d.putCardInHand(me, "Naturalize"), listOf(source)).error shouldBe null
        d.bothPass().error shouldBe null
        d.bothPass().error shouldBe null
        d.powerstones(me).size shouldBe 1
    }

    test("upkeep permission survives destruction of Visions") {
        val d = setup(); val me = d.activePlayer!!
        val land = d.upkeepExile("Forest")
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val source = d.findPermanent(me, VisionsOfPhyrexia.name)!!
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, d.putCardInHand(me, "Naturalize"), listOf(source)).error shouldBe null
        d.bothPass().error shouldBe null
        d.findPermanent(me, VisionsOfPhyrexia.name) shouldBe null
        d.playLand(me, land).error shouldBe null
    }
})
