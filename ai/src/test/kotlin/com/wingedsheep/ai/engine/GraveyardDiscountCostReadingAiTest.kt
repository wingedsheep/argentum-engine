package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.GraveyardSynergy
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Gorex, the Tombshell — "As an additional cost to cast this spell, you may exile any number of
 * creature cards from your graveyard. This spell costs {2} less to cast for each card exiled this way.
 * … Whenever Gorex attacks or dies, choose a card at random exiled with Gorex and put that card into
 * its owner's hand."
 *
 *  - The pile is filled only by the cost, which only ever takes our own graveyard cards, so "its
 *    owner's hand" is ours: the return reads as recursion, not as handing a card back.
 *  - The attack trigger hands back a card every attack: a repeatable card-advantage stream the
 *    in-play prior prices above a vanilla body.
 *  - Exiling creature cards from our graveyard for the discount makes a stocked graveyard a resource.
 */
class GraveyardDiscountCostReadingAiTest : FunSpec({

    fun card(name: String) = MtgSetCatalog.all.flatMap { it.cards }.first { it.name == name }
    val gorex by lazy { card("Gorex, the Tombshell") }

    test("returning a card exiled by its own graveyard cost to hand is recursion, read fully") {
        CardIntentAnalyzer.analyze(gorex).tags shouldContain IntentTag.RECURSION
        withClue("both triggered abilities read fully") {
            AiReadingAudit.read(gorex).abilities.filter { it.checked }.all { it.readFully } shouldBe true
        }
    }

    test("the repeatable attack-trigger return lifts the in-play prior over a vanilla body") {
        CardIntentAnalyzer.analyzeInPlay(gorex).staticPriorValue shouldBeGreaterThan CardIntent.UNKNOWN.staticPriorValue
        withClue("a graveyard-only self-return (Bloodghast) streams from the graveyard, not from the permanent") {
            CardIntentAnalyzer.analyzeInPlay(card("Bloodghast")).staticPriorValue shouldBe CardIntent.UNKNOWN.staticPriorValue
        }
    }

    test("a graveyard-exile discount cost makes it a graveyard payoff") {
        GraveyardSynergy.isPayoff(gorex) shouldBe true
        SynergyAxes.of(gorex).rewards shouldContain SynergyAxis.GRAVEYARD
    }
})
