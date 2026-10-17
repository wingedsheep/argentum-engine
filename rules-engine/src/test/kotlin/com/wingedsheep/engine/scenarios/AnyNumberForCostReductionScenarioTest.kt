package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * `AdditionalCost.AnyNumberForCostReduction` — "as an additional cost to cast this spell, you may
 * <sacrifice / tap / exile from your hand> any number of …. This spell costs {N} less to cast for each
 * … this way" — for the two actions Torgaar's sacrifice didn't cover (its own test pins SACRIFICE).
 *
 * Pinned here:
 *  - the count is a **discount on the total cost**, which includes the announced X (CR 601.2f), so it
 *    pays into an {X}; the effect still reads the announced X;
 *  - it takes **generic mana only** — the coloured pip is always owed;
 *  - choosing none is legal and pays full price;
 *  - exile from hand: only matching cards, never the spell itself (it is on the stack, CR 601.2a);
 *  - tap: only untapped permanents (CR 701.26a), summoning-sick ones included (a cost isn't {T});
 *  - the cast is offered when the *largest* discount makes it affordable, with a zero-floor picker.
 */
class AnyNumberForCostReductionScenarioTest : FunSpec({

    val whiteMarch = card("Test White March") {
        manaCost = "{X}{W}"
        typeLine = "Instant"
        additionalCost(Costs.additional.ExileAnyNumberFromHandForCostReduction(GameObjectFilter.Any.withColor(Color.WHITE), 2))
        spell { effect = Effects.GainLife(DynamicAmount.XValue) }
    }

    val singularity = card("Test Singularity") {
        manaCost = "{4}{R}{R}"
        typeLine = "Sorcery"
        additionalCost(Costs.additional.TapAnyNumberForCostReduction(GameObjectFilter.Creature, 1))
        spell { effect = Effects.GainLife(10) }
    }

    val whiteCard = card("Test White Card") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human"
        power = 1
        toughness = 1
    }

    val redCard = card("Test Red Card") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Goblin"
        power = 1
        toughness = 1
    }

    fun driver(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(whiteMarch, singularity, whiteCard, redCard))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver to me
    }

    fun GameTestDriver.cast(me: EntityId, spell: EntityId, x: Int? = null, payment: AdditionalCostPayment?) =
        submit(CastSpell(me, spell, xValue = x, additionalCostPayment = payment, paymentStrategy = PaymentStrategy.FromPool))

    context("exile from hand") {
        test("each white card exiled takes {2} off the total cost, X included; the effect reads the announced X") {
            val (d, me) = driver()
            val march = d.putCardInHand(me, "Test White March")
            val a = d.putCardInHand(me, "Test White Card")
            val b = d.putCardInHand(me, "Test White Card")
            d.giveMana(me, Color.WHITE, 1) // {4}{W} − {4} = {W}

            d.cast(me, march, x = 4, payment = AdditionalCostPayment(exiledCards = listOf(a, b))).outcome shouldBe Outcome.Done
            d.getExile(me) shouldContainExactlyInAnyOrder listOf(a, b)
            d.bothPass()
            d.getLifeTotal(me) shouldBe 24
        }

        test("the discount never pays the coloured pip") {
            val (d, me) = driver()
            val march = d.putCardInHand(me, "Test White March")
            val cards = List(3) { d.putCardInHand(me, "Test White Card") }
            d.giveColorlessMana(me, 1)

            // {1}{W} − {6}: the generic is gone, the {W} is still owed — colourless can't pay it.
            (d.cast(me, march, x = 1, payment = AdditionalCostPayment(exiledCards = cards)).outcome is Outcome.Rejected) shouldBe true
            d.getExile(me) shouldBe emptyList()
        }

        test("choosing none pays full price") {
            val (d, me) = driver()
            val march = d.putCardInHand(me, "Test White March")
            val white = d.putCardInHand(me, "Test White Card")
            d.giveMana(me, Color.WHITE, 3)

            d.cast(me, march, x = 2, payment = AdditionalCostPayment()).outcome shouldBe Outcome.Done
            (white in d.getHand(me)) shouldBe true
        }

        test("a card that doesn't match the filter is rejected") {
            val (d, me) = driver()
            val march = d.putCardInHand(me, "Test White March")
            val red = d.putCardInHand(me, "Test Red Card")
            d.giveMana(me, Color.WHITE, 1)

            (d.cast(me, march, x = 2, payment = AdditionalCostPayment(exiledCards = listOf(red))).outcome is Outcome.Rejected) shouldBe true
        }

        test("the spell can't exile itself to pay its own cost") {
            val (d, me) = driver()
            val march = d.putCardInHand(me, "Test White March")
            d.giveMana(me, Color.WHITE, 1)

            (d.cast(me, march, x = 2, payment = AdditionalCostPayment(exiledCards = listOf(march))).outcome is Outcome.Rejected) shouldBe true
        }

        test("the legal action offers a zero-floor hand picker of the matching cards only") {
            val (d, me) = driver()
            val march = d.putCardInHand(me, "Test White March")
            val white = d.putCardInHand(me, "Test White Card")
            d.putCardInHand(me, "Test Red Card")
            d.giveMana(me, Color.WHITE, 1)

            val info = d.legalActions(me).first { (it.action as? CastSpell)?.cardId == march }.additionalCostInfo
            info.shouldNotBeNull()
            info.costType shouldBe "ExileFromHand"
            info.exileMinCount shouldBe 0
            // Plains in the opening hand are colourless, so the white card is the only candidate.
            info.validExileTargets shouldBe listOf(white)
        }

        test("the X ceiling counts the discount — one white card to exile lets {W} reach X = 2") {
            val (d, me) = driver()
            val march = d.putCardInHand(me, "Test White March")
            d.putCardInHand(me, "Test White Card")
            d.giveMana(me, Color.WHITE, 1)

            d.legalActions(me).first { (it.action as? CastSpell)?.cardId == march }.maxAffordableX shouldBe 2
        }
    }

    context("tap") {
        test("each creature tapped takes {1} off; summoning-sick creatures may pay") {
            val (d, me) = driver()
            val spell = d.putCardInHand(me, "Test Singularity")
            val creatures = List(4) { d.putCreatureOnBattlefield(me, "Test Red Card") } // all summoning sick
            d.giveMana(me, Color.RED, 2)

            d.cast(me, spell, payment = AdditionalCostPayment(tappedPermanents = creatures)).outcome shouldBe Outcome.Done
            creatures.forEach { d.isTapped(it) shouldBe true }
            d.bothPass()
            d.getLifeTotal(me) shouldBe 30
        }

        test("an already-tapped creature can't pay") {
            val (d, me) = driver()
            val spell = d.putCardInHand(me, "Test Singularity")
            val creatures = List(4) { d.putCreatureOnBattlefield(me, "Test Red Card") }
            d.tapPermanent(creatures[0])
            d.giveMana(me, Color.RED, 2)

            (d.cast(me, spell, payment = AdditionalCostPayment(tappedPermanents = creatures)).outcome is Outcome.Rejected) shouldBe true
        }

        test("offered when the largest discount makes it affordable, with untapped creatures as candidates") {
            val (d, me) = driver()
            val spell = d.putCardInHand(me, "Test Singularity")
            val untapped = List(4) { d.putCreatureOnBattlefield(me, "Test Red Card") }
            val tapped = d.putCreatureOnBattlefield(me, "Test Red Card")
            d.tapPermanent(tapped)
            d.giveMana(me, Color.RED, 2)

            val cast = d.legalActions(me).first { (it.action as? CastSpell)?.cardId == spell }
            cast.affordable shouldBe true
            val info = cast.additionalCostInfo.shouldNotBeNull()
            info.costType shouldBe "TapForCostReduction"
            info.validTapTargets shouldContainExactlyInAnyOrder untapped
        }

        test("a mana creature tapped for the discount isn't also counted as mana") {
            val (d, me) = driver()
            val spell = d.putCardInHand(me, "Test Singularity")
            repeat(2) { d.putLandOnBattlefield(me, "Mountain") }
            // Three untapped Llanowar Elves: tapped for mana they make {3}, tapped for the cost they
            // take {3} off — never both, so {4}{R}{R} is a mana short either way.
            repeat(3) { d.removeSummoningSickness(d.putCreatureOnBattlefield(me, "Llanowar Elves")) }

            d.legalActions(me).none { (it.action as? CastSpell)?.cardId == spell && it.affordable } shouldBe true
        }

        test("not affordable when even every creature can't close the gap") {
            val (d, me) = driver()
            val spell = d.putCardInHand(me, "Test Singularity")
            repeat(3) { d.putCreatureOnBattlefield(me, "Test Red Card") }
            d.giveMana(me, Color.RED, 2)

            d.legalActions(me).none { (it.action as? CastSpell)?.cardId == spell && it.affordable } shouldBe true
        }
    }
})
