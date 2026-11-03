package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.trk.cards.TellMeWhatYouSee
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tell Me What You See {1}{B} — Enchantment
 *   When this enchantment enters, target opponent may have you draw a card. If they don't, they
 *   lose 4 life and you surveil 1.
 *   Whenever you face a dilemma, you may return this enchantment to its owner's hand.
 *
 * Pins the delegated punisher choice: the targeted opponent answers the yes/no, but the draw and
 * the surveil are the enchantment controller's, and the 4 life loss is the opponent's.
 */
class TellMeWhatYouSeeScenarioTest : FunSpec({

    class Table(val d: GameTestDriver, val me: EntityId, val opp: EntityId)

    /** Cast the enchantment and resolve it up to the opponent's yes/no. */
    fun castToPrompt(): Table {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + TellMeWhatYouSee)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40))
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val card = d.putCardInHand(me, "Tell Me What You See")
        d.giveMana(me, Color.BLACK, 2)
        d.castSpell(me, card).error shouldBe null

        var guard = 0
        while (guard++ < 20) {
            when (val pending = d.pendingDecision) {
                is YesNoDecision -> break
                is ChooseTargetsDecision -> d.submitTargetSelection(pending.playerId, listOf(opp)).error shouldBe null
                else -> d.bothPass()
            }
        }
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>().playerId shouldBe opp
        return Table(d, me, opp)
    }

    test("the opponent lets you draw - you draw, nobody loses life") {
        val t = castToPrompt()
        val myHand = t.d.getHandSize(t.me)
        val oppHand = t.d.getHandSize(t.opp)
        val oppLife = t.d.getLifeTotal(t.opp)

        t.d.submitYesNo(t.opp, true).error shouldBe null

        withClue("the enchantment's controller draws") { t.d.getHandSize(t.me) shouldBe myHand + 1 }
        withClue("the deciding opponent doesn't draw") { t.d.getHandSize(t.opp) shouldBe oppHand }
        t.d.getLifeTotal(t.opp) shouldBe oppLife
        t.d.stackSize shouldBe 0
    }

    test("the opponent refuses - they lose 4 life and you surveil 1") {
        val t = castToPrompt()
        val myHand = t.d.getHandSize(t.me)
        val oppLife = t.d.getLifeTotal(t.opp)
        val myLife = t.d.getLifeTotal(t.me)
        val myGraveyard = t.d.getGraveyard(t.me).size

        t.d.submitYesNo(t.opp, false).error shouldBe null

        withClue("the opponent loses 4 life") { t.d.getLifeTotal(t.opp) shouldBe oppLife - 4 }
        t.d.getLifeTotal(t.me) shouldBe myLife
        withClue("no card is drawn") { t.d.getHandSize(t.me) shouldBe myHand }

        val surveil = t.d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("the enchantment's controller surveils") { surveil.playerId shouldBe t.me }
        t.d.submitCardSelection(t.me, surveil.options.take(1)).error shouldBe null
        t.d.getGraveyard(t.me).size shouldBe myGraveyard + 1
    }
})
