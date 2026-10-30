package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `manaCostIs` compares the printed mana cost symbol for symbol, not the mana value. The Urza's
 * Saga ruling is the spec: "you can find only a card with actual mana cost {0} or {1}, not mana
 * value 0 or 1 … you couldn't find a card with mana cost {U} or one with mana cost {X}", and a
 * card with no mana cost (CR 202.1b) is not a card with mana cost {0}.
 */
class ManaCostIsPredicateTest : FunSpec({

    fun artifact(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Artifact"
    }

    val zero = artifact("Zero Trinket", "{0}")
    val one = artifact("One Trinket", "{1}")
    val blue = artifact("Blue Trinket", "{U}")
    val colorless = artifact("Colorless Trinket", "{C}")
    val xCost = artifact("X Trinket", "{X}")
    val two = artifact("Two Trinket", "{2}")
    val noCost = card("Costless Trinket") {
        manaCost = ""
        typeLine = "Artifact"
    }

    val filter = GameObjectFilter.Artifact.manaCostIs("{0}", "{1}")

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(zero, one, blue, colorless, xCost, two, noCost))
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.matches(entity: EntityId, asker: EntityId) =
        services.predicateEvaluator.matches(
            state, state.projectedState, entity, filter, PredicateContext(controllerId = asker)
        )

    test("printed {0} and {1} match in the library") {
        val d = driver(); val me = d.activePlayer!!
        d.matches(d.putCardOnTopOfLibrary(me, zero.name), me) shouldBe true
        d.matches(d.putCardOnTopOfLibrary(me, one.name), me) shouldBe true
    }

    test("same mana value but a different printed cost does not match") {
        val d = driver(); val me = d.activePlayer!!
        d.matches(d.putCardOnTopOfLibrary(me, blue.name), me) shouldBe false
        d.matches(d.putCardOnTopOfLibrary(me, colorless.name), me) shouldBe false
        d.matches(d.putCardOnTopOfLibrary(me, xCost.name), me) shouldBe false
        d.matches(d.putCardOnTopOfLibrary(me, two.name), me) shouldBe false
    }

    test("a card with no mana cost is not a card with mana cost {0}") {
        val d = driver(); val me = d.activePlayer!!
        d.matches(d.putCardOnTopOfLibrary(me, noCost.name), me) shouldBe false
    }

    test("the predicate reads a permanent on the battlefield too") {
        val d = driver(); val me = d.activePlayer!!
        d.matches(d.putPermanentOnBattlefield(me, one.name), me) shouldBe true
        d.matches(d.putPermanentOnBattlefield(me, blue.name), me) shouldBe false
    }

    test("one cost builds a single predicate, several build an Or") {
        GameObjectFilter.Artifact.manaCostIs("{1}").cardPredicates.last() shouldBe
            CardPredicate.ManaCostIs(ManaCost.parse("{1}"))
        filter.cardPredicates.last() shouldBe CardPredicate.Or(
            listOf(CardPredicate.ManaCostIs(ManaCost.parse("{0}")), CardPredicate.ManaCostIs(ManaCost.parse("{1}")))
        )
    }
})
