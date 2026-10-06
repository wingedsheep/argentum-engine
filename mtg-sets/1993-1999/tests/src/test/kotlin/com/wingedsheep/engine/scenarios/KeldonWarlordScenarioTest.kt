package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class KeldonWarlordScenarioTest : FunSpec({
    val defender = card("Warlord test defender") {
        manaCost = "{0}"
        typeLine = "Creature — Human"
        power = 0; toughness = 4
        keywords(Keyword.DEFENDER)
    }
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + defender)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("counts itself and non-Wall defenders but excludes Walls and opposing creatures") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        val warlord = d.putCreatureOnBattlefield(me, "Keldon Warlord")
        d.putCreatureOnBattlefield(me, defender.name)
        d.putCreatureOnBattlefield(me, "Wall of Wood")
        d.putCreatureOnBattlefield(other, "Grizzly Bears")
        d.state.projectedState.getPower(warlord) shouldBe 2
        d.state.projectedState.getToughness(warlord) shouldBe 2
    }

    test("size updates when a counted creature dies") {
        val d = driver(); val me = d.activePlayer!!
        val warlord = d.putCreatureOnBattlefield(me, "Keldon Warlord")
        val bear = d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.state.projectedState.getPower(warlord) shouldBe 2
        d.giveMana(me, Color.RED)
        d.castSpell(me, d.putCardInHand(me, "Lightning Bolt"), listOf(bear)).error shouldBe null
        d.bothPass()
        d.state.projectedState.getPower(warlord) shouldBe 1
        d.state.projectedState.getToughness(warlord) shouldBe 1
    }

    // Ruling 2008-08-01: the CDA applies in all zones. In the library it counts the owner's
    // non-Wall creatures, so "a creature card with power 2 or less" (Imperial Recruiter) sees it
    // only while that count is at most 2.
    test("in the library its power is the live count, as a power filter sees it") {
        val d = driver(); val me = d.activePlayer!!
        val warlord = d.putCardOnTopOfLibrary(me, "Keldon Warlord")
        val powerTwoOrLess = GameObjectFilter.Creature.powerAtMost(2)
        fun seen() = d.services.predicateEvaluator.matches(
            d.state, d.state.projectedState, warlord, powerTwoOrLess, PredicateContext(controllerId = me)
        )
        seen() shouldBe true
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.putCreatureOnBattlefield(me, "Wall of Wood")
        seen() shouldBe true
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        seen() shouldBe false
    }
})
