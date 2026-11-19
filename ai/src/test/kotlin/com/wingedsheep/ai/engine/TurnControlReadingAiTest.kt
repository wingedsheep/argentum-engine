package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * "You control target player during that player's next turn" (Worst Fears, Mindslaver): the target
 * is an opponent to play against, and the card is for [IntentTag.TURN_CONTROL] — which the draft
 * heuristic prices — rather than "no intent".
 */
class TurnControlReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }

    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("Worst Fears targets an opponent and reads as controlling their turn") {
        val worstFears = card("Worst Fears")
        TargetPolarityAnalyzer.forSpell(worstFears) shouldBe listOf(TargetPolarity.HARMFUL)
        CardIntentAnalyzer.analyze(worstFears).tags shouldContain IntentTag.TURN_CONTROL
        AiReadingAudit.audit(worstFears).shouldBeEmpty()
        AiCardRating.of(worstFears, listOf("JOU")).intent.unrecognised shouldBe false
        withClue("the spell's heuristic term prices the controlled turn, not zero") {
            LimitedCardRater.explain(worstFears).heuristic.maxOf { it.value } shouldBeGreaterThan 0.0
        }
    }

    test("Mindslaver's sacrifice ability reads the same") {
        CardIntentAnalyzer.analyze(card("Mindslaver")).tags shouldContain IntentTag.TURN_CONTROL
        AiReadingAudit.audit(card("Mindslaver")).shouldBeEmpty()
    }
})
