package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.gtc.cards.Bioshift
import com.wingedsheep.mtg.sets.definitions.lea.cards.ClockworkBeast
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

/**
 * "Move any number of +1/+1 counters from target creature onto another target creature with the
 * same controller" — `Effects.ChooseNumberThen(then = MoveCounters(...))`. The number only sizes
 * the move, so the readers descend into the body: the move reads either way on both slots (the
 * counters carry the meaning), and +1/+1 counters landing on a creature is a pump.
 */
class ChooseNumberMoveCountersPolarityAiTest : FunSpec({

    test("a chosen-number counter move reads either way on its slots") {
        val polarities = TargetPolarityAnalyzer.forSpell(Bioshift)
        polarities.shouldNotBeEmpty()
        polarities.forEach { it shouldBe TargetPolarity.FLEXIBLE }
    }

    test("moving +1/+1 counters onto a creature is a pump") {
        CardIntentAnalyzer.analyze(Bioshift).tags shouldContain IntentTag.PUMP
    }

    test("Bioshift reads fully") {
        val reading = AiReadingAudit.read(Bioshift)
        withClue("$reading") { reading.readFully shouldBe true }
    }

    test("a chosen number of capped +1/+0 counters on itself reads as self-growth") {
        // Clockwork Beast: ChooseNumberThen(AddCountersWithLimit(+1/+0, X, 7, Self)) — the cap only
        // bounds the count, so it reads like a plain counter on Self: no intent needed.
        val reading = AiReadingAudit.read(ClockworkBeast)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
