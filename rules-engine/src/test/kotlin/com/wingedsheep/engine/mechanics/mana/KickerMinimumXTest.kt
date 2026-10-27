package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * "Kicker {X}. X can't be 0" — [KeywordAbility.OptionalAdditionalCost.minimumXValue] floors the
 * X of a cast that pays the cost; an unfloored Kicker {X} keeps X = 0 legal.
 */
class KickerMinimumXTest : FunSpec({
    val land = card("Island") { typeLine = "Basic Land — Island" }
    val floored = card("Floored Kicker Creature") {
        manaCost = "{U}"; typeLine = "Creature — Merfolk"; power = 1; toughness = 1
        keywordAbility(KeywordAbility.kicker("{X}", minimumXValue = 1))
    }
    val open = card("Open Kicker Creature") {
        manaCost = "{U}"; typeLine = "Creature — Merfolk"; power = 1; toughness = 1
        keywordAbility(KeywordAbility.kicker("{X}"))
    }
    val recurring = card("Recurring Floored Kicker Creature") {
        manaCost = "{U}"; typeLine = "Creature — Merfolk"; power = 1; toughness = 1
        keywordAbility(KeywordAbility.kicker("{X}", minimumXValue = 1))
        staticAbility { ability = MayCastSelfFromZones(zones = listOf(Zone.GRAVEYARD)) }
    }
    fun setup() = GameTestDriver().apply {
        registerCards(listOf(land, floored, open, recurring))
        initMirrorMatch(Deck.of("Island" to 30))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.kickedOffer(p: com.wingedsheep.sdk.model.EntityId, id: com.wingedsheep.sdk.model.EntityId) =
        legalActions(p).single { (it.action as? CastSpell)?.let { a -> a.cardId == id && a.declaredCostSlot == ChoiceSlot.KICKED } == true }

    test("the kicked offer starts its X choice at the floor") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCardInHand(p, floored.name)
        d.giveMana(p, Color.BLUE, 3)
        val offer = d.kickedOffer(p, id)
        offer.affordable shouldBe true
        offer.minX shouldBe 1
        offer.maxAffordableX shouldBe 2
    }
    test("the kicked offer is unaffordable when the ceiling is below the floor") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCardInHand(p, floored.name)
        d.giveMana(p, Color.BLUE, 1)
        d.kickedOffer(p, id).affordable shouldBe false
    }
    test("a kicked cast announcing less than the floor is rejected") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCardInHand(p, floored.name)
        d.giveMana(p, Color.BLUE, 3)
        d.submit(CastSpell(p, id, xValue = 0, declaredCostSlot = ChoiceSlot.KICKED)).error shouldNotBe null
        d.submit(CastSpell(p, id, xValue = 1, declaredCostSlot = ChoiceSlot.KICKED)).error shouldBe null
    }
    test("an unkicked cast is unaffected by the floor") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCardInHand(p, floored.name)
        d.giveMana(p, Color.BLUE, 1)
        d.submit(CastSpell(p, id)).error shouldBe null
    }
    test("an unfloored Kicker {X} still allows X = 0") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCardInHand(p, open.name)
        d.giveMana(p, Color.BLUE, 1)
        d.kickedOffer(p, id).minX shouldBe 0
        d.submit(CastSpell(p, id, xValue = 0, declaredCostSlot = ChoiceSlot.KICKED)).error shouldBe null
    }
    test("a kicked cast from the graveyard carries the X range and the floor") {
        val d = setup(); val p = d.activePlayer!!
        val id = d.putCardInGraveyard(p, recurring.name)
        d.giveMana(p, Color.BLUE, 3)
        val offer = d.kickedOffer(p, id)
        offer.affordable shouldBe true
        offer.hasXCost shouldBe true
        offer.minX shouldBe 1
        offer.maxAffordableX shouldBe 2
        d.submit(CastSpell(p, id, xValue = 0, declaredCostSlot = ChoiceSlot.KICKED)).error shouldNotBe null
        d.submit(CastSpell(p, id, xValue = 2, declaredCostSlot = ChoiceSlot.KICKED)).error shouldBe null
    }
    test("a floor needs an {X} in the kicker's mana cost") {
        shouldThrow<IllegalArgumentException> { KeywordAbility.kicker("{2}", minimumXValue = 1) }
    }
})
