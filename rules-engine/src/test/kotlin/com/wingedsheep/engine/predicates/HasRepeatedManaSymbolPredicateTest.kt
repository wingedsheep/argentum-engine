package com.wingedsheep.engine.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `hasRepeatedManaSymbol` reads the printed mana cost's exact symbols. Jegantha, the Wellspring's
 * ruling is the spec: "If any one card has the same symbol twice, such as {X}{X}{R} or
 * {R/G}{R/G}, the companion condition isn't satisfied" — so a hybrid symbol is its own symbol,
 * distinct from either colour, and a numeric generic symbol is one symbol however large.
 */
class HasRepeatedManaSymbolPredicateTest : FunSpec({

    fun artifact(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Artifact"
    }

    val doubleGreen = artifact("Double Green", "{1}{G}{G}")
    val doubleX = artifact("Double X", "{X}{X}{R}")
    val doubleHybrid = artifact("Double Hybrid", "{R/G}{R/G}")
    val doublePhyrexian = artifact("Double Phyrexian", "{G/P}{G/P}")
    val bigGeneric = artifact("Big Generic", "{4}{R/G}")
    val hybridAndColor = artifact("Hybrid And Color", "{R}{R/G}")
    val phyrexianAndColor = artifact("Phyrexian And Color", "{G}{G/P}")
    val twoColors = artifact("Two Colors", "{W}{U}")
    val noCost = card("Costless Trinket") {
        manaCost = ""
        typeLine = "Artifact"
    }

    val filter = GameObjectFilter.Any.hasRepeatedManaSymbol()

    fun driver() = GameTestDriver().apply {
        registerCards(
            TestCards.all + listOf(
                doubleGreen, doubleX, doubleHybrid, doublePhyrexian, bigGeneric,
                hybridAndColor, phyrexianAndColor, twoColors, noCost,
            )
        )
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.matches(entity: EntityId, asker: EntityId) =
        services.predicateEvaluator.matches(
            state, state.projectedState, entity, filter, PredicateContext(controllerId = asker)
        )

    test("the same symbol twice matches — colored, {X}, hybrid and Phyrexian alike") {
        val d = driver(); val me = d.activePlayer!!
        for (c in listOf(doubleGreen, doubleX, doubleHybrid, doublePhyrexian)) {
            d.matches(d.putCardOnTopOfLibrary(me, c.name), me) shouldBe true
        }
    }

    test("distinct symbols do not match — a hybrid or Phyrexian symbol differs from its color, {4} is one symbol") {
        val d = driver(); val me = d.activePlayer!!
        for (c in listOf(bigGeneric, hybridAndColor, phyrexianAndColor, twoColors)) {
            d.matches(d.putCardOnTopOfLibrary(me, c.name), me) shouldBe false
        }
    }

    test("a card with no mana cost has nothing to repeat") {
        val d = driver(); val me = d.activePlayer!!
        d.matches(d.putCardOnTopOfLibrary(me, noCost.name), me) shouldBe false
    }

    test("the predicate reads a permanent on the battlefield too") {
        val d = driver(); val me = d.activePlayer!!
        d.matches(d.putPermanentOnBattlefield(me, doubleGreen.name), me) shouldBe true
        d.matches(d.putPermanentOnBattlefield(me, twoColors.name), me) shouldBe false
    }
})
