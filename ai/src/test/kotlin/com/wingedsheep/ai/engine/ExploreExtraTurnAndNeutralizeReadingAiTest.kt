package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Readings the Rivals of Ixalan blue/green batch surfaced:
 *
 *  - **Explore** is card selection (keep or bin the top card), plus a pump when another creature
 *    explores (Enter the Unknown) — a gift to the explorer's side. A creature exploring itself
 *    (Jadelight Ranger) grows itself, which needs no tag, and feeds +1/+1-counter and graveyard decks.
 *  - **An extra turn** for its controller reads [IntentTag.EXTRA_TURN] (Timestream Navigator), and the
 *    draft heuristic prices it; one handed to a target opponent (Emrakul, the Promised End) does not.
 *  - **Losing all abilities, or a base of 0/1**, on another creature shuts it off: Warkite Marauder's
 *    attack trigger reads [IntentTag.NEUTRALIZE].
 *  - **A trigger granted for good** that helps its holder is that creature's ability from then on:
 *    Cherished Hatchling's Dinosaurs gaining an enters-fight are fight removal.
 *  - **Handing an exile pile back to its owners** (Induced Amnesia leaving) is the release half of an
 *    exile, needing no intent — while Bag of Holding returning its pile to *our* hand stays a gap.
 */
class ExploreExtraTurnAndNeutralizeReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }

    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")
    fun tags(name: String) = CardIntentAnalyzer.analyze(card(name)).tags
    fun flags(name: String) = AiReadingAudit.audit(card(name)).map { it.key }

    test("explore is card selection, and a pump when another creature explores") {
        withClue("Enter the Unknown's explorer is a creature of ours to help") {
            TargetPolarityAnalyzer.forSpell(card("Enter the Unknown")) shouldBe listOf(TargetPolarity.BENEFICIAL)
            tags("Enter the Unknown") shouldContainAll listOf(IntentTag.CARD_SELECTION, IntentTag.PUMP)
            flags("Enter the Unknown").shouldBeEmpty()
        }
        withClue("Jadelight Ranger exploring itself is card selection and self-growth, read fully") {
            tags("Jadelight Ranger") shouldContain IntentTag.CARD_SELECTION
            tags("Jadelight Ranger") shouldNotContain IntentTag.PUMP
            flags("Jadelight Ranger").shouldBeEmpty()
            AiCardRating.of(card("Jadelight Ranger"), listOf("RIX")).intent.unrecognised shouldBe false
            SynergyAxes.of(card("Jadelight Ranger")).feeds shouldContainAll listOf(SynergyAxis.PLUS_COUNTERS, SynergyAxis.GRAVEYARD)
        }
    }

    test("an extra turn for its controller reads as one and is priced by the draft heuristic") {
        tags("Timestream Navigator") shouldContain IntentTag.EXTRA_TURN
        flags("Timestream Navigator").shouldBeEmpty()
        AiCardRating.of(card("Timestream Navigator"), listOf("RIX")).intent.unrecognised shouldBe false
        withClue("the activated ability's heuristic term prices the extra turn, not zero") {
            LimitedCardRater.explain(card("Timestream Navigator")).heuristic
                .filter { it.part.startsWith("Activated") }.maxOf { it.value } shouldBeGreaterThan 0.0
        }
        withClue("Time Warp's 'target player takes an extra turn' is cast to take it ourselves") {
            tags("Time Warp") shouldContain IntentTag.EXTRA_TURN
        }
        withClue("Emrakul, the Promised End hands its target opponent the extra turn: a price, not a payoff") {
            tags("Emrakul, the Promised End") shouldNotContain IntentTag.EXTRA_TURN
        }
    }

    test("losing all abilities and a 0/1 base on an opposing creature neutralizes it") {
        tags("Warkite Marauder") shouldContain IntentTag.NEUTRALIZE
        tags("Warkite Marauder") shouldNotContain IntentTag.REMOVAL
        flags("Warkite Marauder").shouldBeEmpty()
        AiCardRating.of(card("Warkite Marauder"), listOf("RIX")).intent.unrecognised shouldBe false
    }

    test("a helpful trigger granted for good reads as the ability it grants") {
        tags("Cherished Hatchling") shouldContainAll listOf(IntentTag.REMOVAL, IntentTag.FIGHT)
        flags("Cherished Hatchling").shouldBeEmpty()
    }

    test("an exile pile handed back to its owners needs no intent; one returned to our hand still does") {
        flags("Induced Amnesia").shouldBeEmpty()
        flags("Bag of Holding") shouldContain "intent: Pipeline(FromLinkedExile→hand)"
    }
})
