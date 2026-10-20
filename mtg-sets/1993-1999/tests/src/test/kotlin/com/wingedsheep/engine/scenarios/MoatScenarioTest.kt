package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Jump
import com.wingedsheep.mtg.sets.definitions.leg.cards.Moat
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class MoatScenarioTest : FunSpec({

    val bird = card("Moat Test Bird") {
        manaCost = "{1}{U}"
        typeLine = "Creature — Bird"
        power = 1
        toughness = 1
        keywords(Keyword.FLYING)
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(Moat, Jump, bird))
        initMirrorMatch(Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("creatures without flying can't attack; flyers can, on either side") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(opp, "Moat")
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears").also(d::removeSummoningSickness)
        val flyer = d.putCreatureOnBattlefield(me, bird.name).also(d::removeSummoningSickness)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(bears), opp).error shouldNotBe null
        d.declareAttackers(me, listOf(flyer), opp).error shouldBe null
    }

    test("Jump cast after Moat entered lets a ground creature attack") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.putPermanentOnBattlefield(me, "Moat")
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears").also(d::removeSummoningSickness)

        val jump = d.putCardInHand(me, "Jump")
        d.giveMana(me, Color.BLUE, 1)
        d.castSpell(me, jump, listOf(bears)).error shouldBe null
        d.bothPass()

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(bears), opp).error shouldBe null
    }
})
