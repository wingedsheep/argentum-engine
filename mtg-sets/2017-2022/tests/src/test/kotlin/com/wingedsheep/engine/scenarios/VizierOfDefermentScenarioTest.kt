package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Vizier of Deferment (AKH #37) — {2}{W} 2/2 Flash.
 *
 * "When this creature enters, you may exile target creature if it attacked or blocked this turn.
 *  Return that card to the battlefield under its owner's control at the beginning of the next end step."
 *
 * The attacked-or-blocked check runs on resolution against the target's turn history, and the
 * "may" is only offered when it passes.
 */
class VizierOfDefermentScenarioTest : FunSpec({

    fun createDriver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.flashInVizier(caster: EntityId) {
        val vizier = putCardInHand(caster, "Vizier of Deferment")
        giveMana(caster, Color.WHITE); giveColorlessMana(caster, 2)
        castSpell(caster, vizier).error shouldBe null
        bothPass().error shouldBe null // resolve the creature spell; the enters trigger wants a target
    }

    test("exiles a creature that attacked this turn and returns it at the next end step") {
        val d = createDriver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val bears = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.removeSummoningSickness(bears)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(me, listOf(bears), opp).error shouldBe null

        d.flashInVizier(me)
        d.submitTargetSelection(me, listOf(bears)).error shouldBe null
        d.bothPass().error shouldBe null // resolve the enters trigger

        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(me, true).error shouldBe null
        d.getExileCardNames(me) shouldContain "Grizzly Bears"
        d.findPermanent(me, "Grizzly Bears") shouldBe null

        d.passPriorityUntil(Step.END)
        d.bothPass() // resolve the delayed return trigger
        d.findPermanent(me, "Grizzly Bears") shouldNotBe null
        d.getLifeTotal(opp) shouldBe 20 // removed from combat before damage
    }

    test("does nothing to a creature that neither attacked nor blocked") {
        val d = createDriver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val oppBears = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        d.flashInVizier(me)
        d.submitTargetSelection(me, listOf(oppBears)).error shouldBe null
        d.bothPass().error shouldBe null // resolve the enters trigger

        (d.pendingDecision is YesNoDecision) shouldBe false
        d.getPermanents(opp) shouldContain oppBears
    }
})
