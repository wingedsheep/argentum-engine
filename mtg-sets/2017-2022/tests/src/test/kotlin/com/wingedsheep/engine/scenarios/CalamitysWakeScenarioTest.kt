package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bro.cards.CalamitysWake
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CalamitysWakeScenarioTest : FunSpec({
    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + CalamitysWake)
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun castWake(d: GameTestDriver): EntityId {
        val me = d.activePlayer!!
        val wake = d.putCardInHand(me, "Calamity's Wake")
        d.giveMana(me, Color.WHITE, 2)
        d.castSpell(me, wake).outcome shouldBe Outcome.Done
        d.bothPass()
        return wake
    }
    test("exiles both graveyards and itself, then forbids both players' noncreature spells") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val mine = d.putCardInGraveyard(me, "Grizzly Bears")
        val theirs = d.putCardInGraveyard(opp, "Lightning Bolt")
        val wake = castWake(d)
        d.state.getZone(ZoneKey(me, Zone.GRAVEYARD)).isEmpty() shouldBe true
        d.state.getZone(ZoneKey(opp, Zone.GRAVEYARD)).isEmpty() shouldBe true
        d.state.getZone(ZoneKey(me, Zone.EXILE)).containsAll(listOf(mine, wake)) shouldBe true
        d.state.getZone(ZoneKey(opp, Zone.EXILE)).contains(theirs) shouldBe true
        for (player in listOf(me, opp)) {
            if (d.state.priorityPlayerId != player) d.passPriority(d.state.priorityPlayerId!!)
            val bolt = d.putCardInHand(player, "Lightning Bolt")
            d.giveMana(player, Color.RED)
            d.legalActions(player).any { (it.action as? CastSpell)?.cardId == bolt } shouldBe false
            (d.castSpell(player, bolt, listOf(d.getOpponent(player))).error != null) shouldBe true
        }
    }
    test("empty graveyards do not prevent the ban, creatures and land plays remain legal") {
        val d = setup(); val me = d.activePlayer!!
        castWake(d)
        val land = d.putCardInHand(me, "Forest")
        d.playLand(me, land).outcome shouldBe Outcome.Done
        val body = d.putCardInHand(me, "Grizzly Bears")
        d.giveMana(me, Color.GREEN, 2)
        d.castSpell(me, body).outcome shouldBe Outcome.Done
        d.bothPass()
        d.assertPermanentExists(me, "Grizzly Bears")
    }
    test("a noncreature spell already on the stack still resolves") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        val bolt = d.putCardInHand(me, "Lightning Bolt")
        d.giveMana(me, Color.RED)
        d.castSpell(me, bolt, listOf(opp)).outcome shouldBe Outcome.Done
        val wake = castWake(d)
        d.state.stack.contains(bolt) shouldBe true
        d.state.getZone(ZoneKey(me, Zone.EXILE)).contains(wake) shouldBe true
        d.bothPass()
        d.getLifeTotal(opp) shouldBe 17
        d.assertInGraveyard(me, "Lightning Bolt")
    }
    test("the ban ends at cleanup even though Wake remains exiled") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        castWake(d)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, activePlayer = opp)
        val bolt = d.putCardInHand(opp, "Lightning Bolt")
        d.giveMana(opp, Color.RED)
        d.legalActions(opp).any { (it.action as? CastSpell)?.cardId == bolt } shouldBe true
        d.castSpell(opp, bolt, listOf(me)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.getLifeTotal(me) shouldBe 17
    }
})
