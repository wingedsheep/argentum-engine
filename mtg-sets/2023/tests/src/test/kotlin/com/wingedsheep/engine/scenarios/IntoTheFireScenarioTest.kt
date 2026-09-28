package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mom.cards.IntoTheFire
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class IntoTheFireScenarioTest : FunSpec({

    fun setup(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + IntoTheFire)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        d.giveMana(me, Color.RED, 3)
        return Triple(d, me, d.getOpponent(me))
    }

    test("mode 0 deals 2 damage to each creature") {
        val (d, me, opp) = setup()
        d.putCreatureOnBattlefield(me, "Glory Seeker") // 2/2
        d.putCreatureOnBattlefield(opp, "Centaur Courser") // 3/3
        val spell = d.putCardInHand(me, "Into the Fire")
        d.submit(CastSpell(playerId = me, cardId = spell, chosenModes = listOf(0))).error.shouldBeNull()
        d.bothPass()
        d.findPermanent(me, "Glory Seeker").shouldBeNull()
        d.findPermanent(opp, "Centaur Courser").shouldNotBeNull()
    }

    test("mode 1 bottoms chosen cards and draws that many plus one") {
        val (d, me, _) = setup()
        val spell = d.putCardInHand(me, "Into the Fire")
        d.submit(CastSpell(playerId = me, cardId = spell, chosenModes = listOf(1))).error.shouldBeNull()
        val before = d.getHand(me).size // Into the Fire already on the stack
        d.bothPass()
        val decision = d.pendingDecision as SelectCardsDecision
        d.submitCardSelection(me, decision.options.take(2))
        (d.pendingDecision as? ReorderLibraryDecision)?.let { d.submitOrderedResponse(me, it.cards) }
        d.getHand(me).size shouldBe before - 2 + 3
    }

    test("mode 1 choosing zero cards still draws one") {
        val (d, me, _) = setup()
        val spell = d.putCardInHand(me, "Into the Fire")
        d.submit(CastSpell(playerId = me, cardId = spell, chosenModes = listOf(1))).error.shouldBeNull()
        val before = d.getHand(me).size
        d.bothPass()
        d.submitCardSelection(me, emptyList())
        d.getHand(me).size shouldBe before + 1
    }
})
