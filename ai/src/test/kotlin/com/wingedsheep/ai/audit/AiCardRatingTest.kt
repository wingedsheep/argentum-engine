package com.wingedsheep.ai.audit

import com.wingedsheep.ai.engine.LimitedCardRater
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * The rating half of the Assay explorer's AI panel. The load-bearing claims are that the explained
 * numbers *are* the AI's numbers — [LimitedCardRater.explain] agrees with [LimitedCardRater.rate] on
 * every card in the corpus, and every part list sums to the number it breaks down, so the page's
 * segmented bars never draw a total the AI doesn't use.
 */
class AiCardRatingTest : FunSpec({

    val all by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name } }
    val byName by lazy { all.associateBy { it.name } }
    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("explain gives exactly rate's answer, and the heuristic terms sum to the heuristic, on every card") {
        for (c in all) {
            val e = LimitedCardRater.explain(c)
            e.rating shouldBe LimitedCardRater.rate(c)
            if (e.source == LimitedCardRater.Source.HEURISTIC) {
                e.heuristicRating shouldBe (e.heuristic.sumOf { it.value }.coerceIn(0.0, 5.0) plusOrMinus 1e-9)
            }
        }
    }

    test("a creature's board value is its printed stats plus each keyword") {
        val r = AiCardRating.of(card("Serra Angel"), listOf("LEA"))
        r.board.kind shouldBe AiCardRating.BoardKind.CREATURE
        r.board.parts.map { it.label } shouldContain "Flying"
        r.board.parts.map { it.label } shouldContain "Vigilance"
        r.board.value.shouldNotBeNull() shouldBe (r.board.parts.sumOf { it.value } plusOrMinus 1e-9)
    }

    test("an instant has no board value and reads as removal") {
        val r = AiCardRating.of(card("Lightning Bolt"), listOf("LEA"))
        r.board.kind shouldBe AiCardRating.BoardKind.SPELL
        r.board.value.shouldBeNull()
        r.intent.tags.map { it.key } shouldContain "REMOVAL"
        r.limited.sets.single().set shouldBe "LEA"
    }

    test("a non-creature permanent is priced by the highest prior rung it reaches") {
        val r = AiCardRating.of(card("Icy Manipulator"), emptyList())
        r.board.kind shouldBe AiCardRating.BoardKind.PERMANENT
        r.board.parts.single().label shouldBe "Repeatable tapper"
        r.board.value shouldBe 3.5
    }
})
