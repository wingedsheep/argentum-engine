package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.definitions.mkm.cards.OutrageousRobbery
import com.wingedsheep.mtg.sets.definitions.lea.cards.DemonicTutor
import com.wingedsheep.mtg.sets.definitions.mrd.cards.Leveler
import com.wingedsheep.mtg.sets.definitions.ogw.cards.DimensionalInfiltrator
import com.wingedsheep.mtg.sets.definitions.ogw.cards.InverterOfTruth
import com.wingedsheep.mtg.sets.definitions.ogw.cards.RealitySmasher
import com.wingedsheep.mtg.sets.definitions.ogw.cards.ThoughtHarvester
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * Oath of the Gatewatch shapes the intent reader had no tag for, or the wrong one.
 *
 * - "Target opponent exiles the top card of their library" (Thought Harvester, Dimensional
 *   Infiltrator) is the top-of-library form of exiling cards out of another player's library —
 *   [IntentTag.MILL], as Supreme Inquisitor's searched exile already reads.
 * - Inverter of Truth exiles its controller's whole library face down: a library gather that
 *   picks nothing out is not [IntentTag.TUTOR].
 * - Reality Smasher's "counter that spell unless its controller discards a card" is Ward's tax on
 *   answering the permanent — [IntentTag.PROTECTION].
 */
class OathEldraziIntentAiTest : FunSpec({

    test("an opponent exiling the top of their library is a mill") {
        CardIntentAnalyzer.analyze(ThoughtHarvester).tags shouldContain IntentTag.MILL
        CardIntentAnalyzer.analyze(DimensionalInfiltrator).tags shouldContain IntentTag.MILL
    }

    test("an opponent's top card exiled is not a tutor of ours") {
        CardIntentAnalyzer.analyze(ThoughtHarvester).tags shouldNotContain IntentTag.TUTOR
    }

    test("exiling an opponent's top cards to play them is a theft, not a mill") {
        CardIntentAnalyzer.analyze(OutrageousRobbery).tags shouldNotContain IntentTag.MILL
    }

    test("exiling our whole library face down finds no card: not a tutor") {
        CardIntentAnalyzer.analyze(InverterOfTruth).tags shouldNotContain IntentTag.TUTOR
        CardIntentAnalyzer.analyze(Leveler).tags shouldNotContain IntentTag.TUTOR
        CardIntentAnalyzer.analyze(DemonicTutor).tags shouldContain IntentTag.TUTOR
    }

    test("a ward-style counter on being targeted protects the permanent") {
        CardIntentAnalyzer.analyze(RealitySmasher).tags shouldContain IntentTag.PROTECTION
    }
})
