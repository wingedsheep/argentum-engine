package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.definitions.akh.cards.GrimStrider
import com.wingedsheep.mtg.sets.definitions.chk.cards.CranialExtraction
import com.wingedsheep.mtg.sets.definitions.inv.cards.CrusadingKnight
import com.wingedsheep.mtg.sets.definitions.inv.cards.ExoticCurse
import com.wingedsheep.mtg.sets.definitions.lea.cards.DemonicTutor
import com.wingedsheep.mtg.sets.definitions.ons.cards.SupremeInquisitor
import com.wingedsheep.mtg.sets.definitions.soi.cards.GeralfsMasterpiece
import com.wingedsheep.mtg.sets.definitions.soi.cards.InvasiveSurgery
import com.wingedsheep.mtg.sets.definitions.xln.cards.SettleTheWreckage
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * Two intent misreadings that priced a drawback or a piece of disruption as a gain.
 *
 * - A `GrantDynamicStats` is read by the sign of its amounts, as `ModifyStats` is: "this creature
 *   gets -1/-1 for each card in your hand" (Geralf's Masterpiece, Grim Strider) is the creature's
 *   price, not a pump — read as one, its board value carried a pump's ability prior on top of the
 *   projected body. An Aura's dynamic "-X/-X" (Exotic Curse) answers what it enchants.
 * - A library search is a tutor only when we search our own library. Invasive Surgery's delirium
 *   searches the countered spell's controller's zones (`searcher = ControllerOfSource` inside a
 *   `ForEachPlayer`) to exile copies — disruption; Settle the Wreckage hands the search to the
 *   attacker's controller.
 */
class NegativeSelfStatGrantAndExileSearchIntentAiTest : FunSpec({

    test("a creature's own dynamic shrink is not a pump") {
        CardIntentAnalyzer.analyze(GeralfsMasterpiece).tags shouldNotContain IntentTag.PUMP
        CardIntentAnalyzer.analyze(GrimStrider).tags shouldNotContain IntentTag.PUMP
    }

    test("a dynamic +X/+X on itself is still a pump") {
        CardIntentAnalyzer.analyze(CrusadingKnight).tags shouldContain IntentTag.PUMP
    }

    test("an Aura's dynamic -X/-X answers what it enchants") {
        val tags = CardIntentAnalyzer.analyze(ExoticCurse).tags
        tags shouldNotContain IntentTag.PUMP
        tags shouldContain IntentTag.REMOVAL
    }

    test("searching another player's zones to exile a name is not a tutor") {
        val tags = CardIntentAnalyzer.analyze(InvasiveSurgery).tags
        tags shouldNotContain IntentTag.TUTOR
        tags shouldContain IntentTag.COUNTERSPELL
        CardIntentAnalyzer.analyze(CranialExtraction).tags shouldNotContain IntentTag.TUTOR
    }

    test("exiling cards searched out of another player's library mills them, not a tutor") {
        val tags = CardIntentAnalyzer.analyze(SupremeInquisitor).tags
        tags shouldNotContain IntentTag.TUTOR
        tags shouldContain IntentTag.MILL
    }

    test("a search handed to another player is not our tutor") {
        CardIntentAnalyzer.analyze(SettleTheWreckage).tags shouldNotContain IntentTag.TUTOR
    }

    test("searching our own library is still a tutor") {
        CardIntentAnalyzer.analyze(DemonicTutor).tags shouldContain IntentTag.TUTOR
    }
})
