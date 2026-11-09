package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * "If this spell's madness cost was paid" — [Conditions.MadnessCostWasPaid] over
 * [ChoiceSlot.MADNESS_CAST] (CR 702.35).
 *
 * CR 702.35a's trigger lets the owner "cast it by paying [cost] rather than paying its mana cost",
 * and CR 702.35b says that cast follows the alternative-cost rules of 601.2b and 601.2f–h — so an
 * `{X}` in the madness cost is announced as the cast begins (601.2b), even though the cast happens
 * while the trigger resolves and no client action carried it. Each test pins one of those rules:
 * the flag is set for a madness cast and only for one, X is asked for and spent, and a permanent
 * spell carries the flag onto the battlefield.
 */
class MadnessCostWasPaidTest : FunSpec({

    // Gains 1 life from hand; X life when its madness cost {X}{B} was paid.
    val madnessTithe = card("Madness Tithe") {
        manaCost = "{2}{B}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.If(
                Conditions.MadnessCostWasPaid,
                then = Effects.GainLife(DynamicAmounts.xValue()),
                otherwise = Effects.GainLife(1),
            )
        }
        madness("{X}{B}")
    }

    // A fixed madness cost: the flag alone picks the 5-life branch.
    val madnessToll = card("Madness Toll") {
        manaCost = "{3}{B}"
        typeLine = "Sorcery"
        spell {
            effect = Effects.If(
                Conditions.MadnessCostWasPaid,
                then = Effects.GainLife(5),
                otherwise = Effects.GainLife(2),
            )
        }
        madness("{B}")
    }

    // A madness creature, to prove the flag rides the spell onto the permanent it becomes.
    val madnessHound = card("Madness Hound") {
        manaCost = "{3}{B}"
        typeLine = "Creature — Dog"
        power = 2
        toughness = 2
        madness("{B}")
    }

    val selfDiscard = card("Self Discard") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        spell { effect = Effects.Discard(1) }
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(madnessTithe, madnessToll, madnessHound, selfDiscard))
        initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    /** Discard [cardId] with a "discard a card" sorcery, then let the madness trigger resolve to its prompt. */
    fun GameTestDriver.discardIntoMadness(player: EntityId, cardId: EntityId) {
        val spell = putCardInHand(player, "Self Discard")
        giveColorlessMana(player, 1)
        submitSuccess(CastSpell(player, spell, paymentStrategy = PaymentStrategy.FromPool))
        bothPass()
        submitCardSelection(player, listOf(cardId))
        var guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 20) bothPass()
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    test("a madness cast reads its madness cost as paid") {
        val d = driver()
        val p = d.activePlayer!!
        val toll = d.putCardInHand(p, "Madness Toll")
        val life = d.getLifeTotal(p)
        d.discardIntoMadness(p, toll)
        d.giveMana(p, Color.BLACK, 1)
        d.submitYesNo(p, true)
        d.resolveStack()
        d.getLifeTotal(p) shouldBe life + 5
    }

    test("a cast from hand for its mana cost does not") {
        val d = driver()
        val p = d.activePlayer!!
        val toll = d.putCardInHand(p, "Madness Toll")
        val life = d.getLifeTotal(p)
        d.giveMana(p, Color.BLACK, 4)
        d.castSpell(p, toll).error shouldBe null
        d.resolveStack()
        d.getLifeTotal(p) shouldBe life + 2
    }

    test("an {X} madness cost asks for X as the cast begins and the spell reads it (CR 601.2b)") {
        val d = driver()
        val p = d.activePlayer!!
        val tithe = d.putCardInHand(p, "Madness Tithe")
        val life = d.getLifeTotal(p)
        d.discardIntoMadness(p, tithe)
        d.giveMana(p, Color.BLACK, 4) // {X}{B} with X up to 3
        d.submitYesNo(p, true)

        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        ask.minValue shouldBe 0
        ask.maxValue shouldBe 3
        d.submitDecision(p, NumberChosenResponse(ask.id, 3)).error shouldBe null
        d.resolveStack()

        d.getLifeTotal(p) shouldBe life + 3
        d.getGraveyard(p).contains(tithe) shouldBe true
    }

    test("X = 0 is a legal madness announcement and pays just the coloured part") {
        val d = driver()
        val p = d.activePlayer!!
        val tithe = d.putCardInHand(p, "Madness Tithe")
        val life = d.getLifeTotal(p)
        d.discardIntoMadness(p, tithe)
        d.giveMana(p, Color.BLACK, 1)
        d.submitYesNo(p, true)

        val ask = d.pendingDecision.shouldBeInstanceOf<ChooseNumberDecision>()
        ask.maxValue shouldBe 0
        d.submitDecision(p, NumberChosenResponse(ask.id, 0)).error shouldBe null
        d.resolveStack()

        // The madness branch ran with X = 0, not the 1-life hand branch.
        d.getLifeTotal(p) shouldBe life
    }

    test("a permanent spell cast for its madness cost carries the flag onto the battlefield") {
        val d = driver()
        val p = d.activePlayer!!
        val hound = d.putCardInHand(p, "Madness Hound")
        d.discardIntoMadness(p, hound)
        d.giveMana(p, Color.BLACK, 1)
        d.submitYesNo(p, true)
        d.resolveStack()

        val perm = d.findPermanent(p, "Madness Hound").shouldNotBeNull()
        d.state.getEntity(perm)?.get<CastChoicesComponent>()?.chosen
            ?.containsKey(ChoiceSlot.MADNESS_CAST) shouldBe true
    }

    test("the same creature cast from hand carries no madness flag") {
        val d = driver()
        val p = d.activePlayer!!
        val hound = d.putCardInHand(p, "Madness Hound")
        d.giveMana(p, Color.BLACK, 4)
        d.castSpell(p, hound).error shouldBe null
        d.resolveStack()

        val perm = d.findPermanent(p, "Madness Hound").shouldNotBeNull()
        (d.state.getEntity(perm)?.get<CastChoicesComponent>()?.chosen
            ?.containsKey(ChoiceSlot.MADNESS_CAST) == true) shouldBe false
    }
})
