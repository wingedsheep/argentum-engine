package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.SoulsOfTheLost
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Souls of the Lost — {1}{B} Creature — Spirit (LCI #121).
 *
 * "As an additional cost to cast this spell, discard a card **or** sacrifice a permanent.
 *  Fathomless descent — Souls of the Lost's power is equal to the number of permanent cards in your
 *  graveyard and its toughness is equal to that number plus 1."
 *
 * Exercises the new cost-vs-cost [com.wingedsheep.sdk.scripting.AdditionalCost.Choice] additional
 * cost (both payment paths + the two enumerated cast actions) and the fathomless-descent CDA P/T.
 */
class SoulsOfTheLostScenarioTest : FunSpec({

    fun setup(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(SoulsOfTheLost))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("discard path — pays by discarding a card; CDA P/T counts permanent cards in graveyard") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        // Two permanent cards already in the graveyard, plus a discardable permanent card in hand.
        driver.putCardInGraveyard(p1, "Swamp")
        driver.putCardInGraveyard(p1, "Swamp")
        val souls = driver.putCardInHand(p1, "Souls of the Lost")
        val toDiscard = driver.putCardInHand(p1, "Swamp")
        driver.giveMana(p1, Color.BLACK, 2)

        driver.submit(
            CastSpell(
                playerId = p1,
                cardId = souls,
                additionalCostPayment = AdditionalCostPayment(discardedCards = listOf(toDiscard)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass() // resolve the creature spell

        // Discarded card is now in the graveyard → 3 permanent cards total.
        (toDiscard in driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(p1, Zone.GRAVEYARD))) shouldBe true
        (souls in driver.state.getBattlefield()) shouldBe true
        driver.state.projectedState.getPower(souls) shouldBe 3
        driver.state.projectedState.getToughness(souls) shouldBe 4
    }

    test("sacrifice path — pays by sacrificing a permanent, which itself feeds the CDA count") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        // Empty graveyard; one sacrificeable permanent on the battlefield.
        val fodder = driver.putPermanentOnBattlefield(p1, "Swamp")
        val souls = driver.putCardInHand(p1, "Souls of the Lost")
        driver.giveMana(p1, Color.BLACK, 2)

        driver.submit(
            CastSpell(
                playerId = p1,
                cardId = souls,
                additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)),
                paymentStrategy = PaymentStrategy.FromPool,
            )
        ).error shouldBe null
        driver.bothPass()

        // The sacrificed permanent is now the only permanent card in the graveyard → 1/2.
        (fodder in driver.state.getZone(com.wingedsheep.engine.state.ZoneKey(p1, Zone.GRAVEYARD))) shouldBe true
        (souls in driver.state.getBattlefield()) shouldBe true
        driver.state.projectedState.getPower(souls) shouldBe 1
        driver.state.projectedState.getToughness(souls) shouldBe 2
    }

    test("enumeration — offers exactly one cast action per payable option, no un-costed cast") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        driver.putPermanentOnBattlefield(p1, "Swamp") // sacrifice fodder
        val souls = driver.putCardInHand(p1, "Souls of the Lost")
        driver.putCardInHand(p1, "Swamp") // discard fodder
        driver.giveMana(p1, Color.BLACK, 2)

        val soulsCasts = driver.legalActions(p1).filter {
            (it.action as? CastSpell)?.cardId == souls
        }
        // Two paths — discard and sacrifice — each with its own picker; and never a bare cast that
        // would skip the mandatory additional cost.
        soulsCasts.map { it.additionalCostInfo?.costType } shouldContainExactlyInAnyOrder
            listOf("DiscardCard", "SacrificePermanent")
        soulsCasts.all { it.additionalCostInfo != null } shouldBe true
    }

    test("uncastable — no cast action when no option can be paid") {
        val driver = setup()
        val p1 = driver.activePlayer!!

        // Clear the opening hand so the spell itself is the only card in hand (nothing to discard),
        // and the fresh board has no permanents to sacrifice. Mana is supplied from the pool so mana
        // is never the reason the cast is missing.
        driver.state.getHand(p1).toList().forEach { driver.moveToGraveyard(it) }
        val souls = driver.putCardInHand(p1, "Souls of the Lost")
        driver.giveMana(p1, Color.BLACK, 2)

        val soulsCasts = driver.legalActions(p1).filter {
            (it.action as? CastSpell)?.cardId == souls
        }
        soulsCasts shouldBe emptyList()
        // Sanity: the card really is present; it's just uncastable for want of an additional cost.
        souls shouldNotBe null
    }
})
