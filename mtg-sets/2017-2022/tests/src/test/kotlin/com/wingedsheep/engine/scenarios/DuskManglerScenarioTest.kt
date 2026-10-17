package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.snc.cards.DuskMangler
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Dusk Mangler (SNC #77) — {5}{B}{B} Creature — Horror, 5/4.
 *
 * "As an additional cost to cast this spell, sacrifice a creature, discard a card, or pay 4 life.
 *  When this creature enters, each opponent sacrifices a creature of their choice, discards a card,
 *  and loses 4 life."
 *
 * Covers the three-way cost (one cast action per branch, each paying only its own branch) and the
 * enters trigger hitting the opponent with all three.
 */
class DuskManglerScenarioTest : FunSpec({
    val slot = ChoiceSlot.ADDITIONAL_COST_BRANCH

    fun setup(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + DuskMangler)
        it.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castOffers(spell: EntityId) =
        legalActions(activePlayer!!).filter { (it.action as? CastSpell)?.cardId == spell }

    test("each of the three costs is its own cast offer") {
        val d = setup()
        val p = d.activePlayer!!
        d.putPermanentOnBattlefield(p, "Grizzly Bears")
        d.putCardInHand(p, "Swamp")
        val spell = d.putCardInHand(p, "Dusk Mangler")
        d.giveMana(p, Color.BLACK, 7)
        val offers = d.castOffers(spell)
        offers.map { (it.action as CastSpell).additionalCostChoices[slot] }.toSet() shouldBe setOf(0, 1, 2)
        offers.map { it.additionalCostInfo!!.costType }.toSet() shouldBe setOf("SacrificePermanent", "DiscardCard", "PayLife")
    }

    test("paying 4 life keeps creatures and hand; the enters trigger hits the opponent three ways") {
        val d = setup()
        val p = d.activePlayer!!
        val opponent = d.state.turnOrder.first { it != p }
        val bears = d.putPermanentOnBattlefield(p, "Grizzly Bears")
        val theirBears = d.putPermanentOnBattlefield(opponent, "Grizzly Bears")
        val spell = d.putCardInHand(p, "Dusk Mangler")
        d.giveMana(p, Color.BLACK, 7)
        val handBefore = d.state.getHand(p).size
        val cast = d.castOffers(spell).single { (it.action as CastSpell).additionalCostChoices[slot] == 2 }.action as CastSpell
        d.submit(cast.copy(paymentStrategy = PaymentStrategy.FromPool)).error shouldBe null

        d.getLifeTotal(p) shouldBe 16
        (bears in d.state.getBattlefield()) shouldBe true
        d.state.getHand(p).size shouldBe handBefore - 1

        val theirHand = d.state.getHand(opponent).size
        d.bothPass() // the creature
        d.bothPass() // its enters trigger
        while (d.state.pendingDecision != null) d.autoResolveDecision()

        (theirBears in d.state.getZone(ZoneKey(opponent, Zone.GRAVEYARD))) shouldBe true
        d.state.getHand(opponent).size shouldBe theirHand - 1
        d.getLifeTotal(opponent) shouldBe 16
        d.getLifeTotal(p) shouldBe 16
    }

    test("the sacrifice branch pays with a creature and no life") {
        val d = setup()
        val p = d.activePlayer!!
        val bears = d.putPermanentOnBattlefield(p, "Grizzly Bears")
        val spell = d.putCardInHand(p, "Dusk Mangler")
        d.giveMana(p, Color.BLACK, 7)
        val cast = d.castOffers(spell).single { (it.action as CastSpell).additionalCostChoices[slot] == 0 }.action as CastSpell
        d.submit(cast.copy(
            additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
            paymentStrategy = PaymentStrategy.FromPool,
        )).error shouldBe null
        (bears in d.state.getZone(ZoneKey(p, Zone.GRAVEYARD))) shouldBe true
        d.getLifeTotal(p) shouldBe 20
    }

    test("below 4 life with nothing to sacrifice or discard, it can't be cast") {
        val d = setup()
        val p = d.activePlayer!!
        val spell = d.putCardInHand(p, "Dusk Mangler")
        d.giveMana(p, Color.BLACK, 7)
        d.state.getHand(p).filter { it != spell }.forEach { d.moveToGraveyard(it) }
        d.setLifeTotal(p, 3)
        d.castOffers(spell) shouldBe emptyList()
    }
})
