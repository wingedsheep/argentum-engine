package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.atq.cards.DampingField
import com.wingedsheep.mtg.sets.definitions.kld.cards.DovinBaan
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * An untap cap scoped to opponents — Dovin Baan's emblem, "Your opponents can't untap more than
 * two permanents during their untap steps" ([com.wingedsheep.sdk.scripting.UntapLimitPerStep] with
 * `players = Player.EachOpponent`) — keeps the opponents' permanents tapped: a tapper's lock,
 * [IntentTag.TAPPER]. A symmetric cap (Damping Field) binds its controller too and reads as no plan.
 */
class OpponentUntapCapReadingAiTest : FunSpec({

    test("Dovin Baan reads fully, its emblem as a tapper's lock") {
        val reading = AiReadingAudit.read(DovinBaan)
        withClue("$reading") { reading.readFully shouldBe true }
        CardIntentAnalyzer.analyze(DovinBaan).tags shouldContain IntentTag.TAPPER
    }

    test("Dovin Baan's +1 is aimed at an opposing creature") {
        val plusOne = DovinBaan.script.activatedAbilities[0]
        TargetPolarityAnalyzer.forAbility(DovinBaan, plusOne) shouldBe listOf(TargetPolarity.HARMFUL)
    }

    test("a symmetric untap cap is not read as a tapper") {
        CardIntentAnalyzer.analyze(DampingField).tags shouldNotContain IntentTag.TAPPER
    }
})
