package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The soft-counter band: "Counter target spell unless its controller pays {2}." lands on
 * `Effects.CounterUnlessPays`, the spelling the hand-written soft counters use.
 */
class SoftCounterTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    "soft counters round-trip" {
        for (line in listOf(
            "Counter target spell unless its controller pays {2}.",
            "Counter target noncreature spell unless its controller pays {1}.",
            "Counter target creature spell unless its controller pays {1}{U}.",
            "{U}, Sacrifice ~: Counter target spell unless its controller pays {2}.",
            "Counter target spell unless its controller pays {X}.",
            "Counter target spell unless its controller pays {X}. If that spell is countered this way, " +
                "exile it instead of putting it into its owner's graveyard.",
            "Counter target spell unless its controller pays {3}. If that spell is countered this way, " +
                "exile it instead of putting it into its owner's graveyard.",
        )) {
            Grammar.abilityLine.printLine(fragment(line)) shouldBe line
        }
    }

    "a soft counter lands on CounterUnlessPays" {
        fragment("Counter target spell unless its controller pays {3}.").script.spellEffect shouldBe
            Effects.CounterUnlessPays("{3}")
    }

    "an X tax is the spell's own X, and the exile rider takes the dynamic facade" {
        fragment("Counter target spell unless its controller pays {X}.").script.spellEffect shouldBe
            Effects.CounterUnlessDynamicPays(DynamicAmounts.xValue())
        fragment(
            "Counter target spell unless its controller pays {3}. If that spell is countered this way, " +
                "exile it instead of putting it into its owner's graveyard."
        ).script.spellEffect shouldBe Effects.CounterUnlessDynamicPays(DynamicAmounts.fixed(3), exileOnCounter = true)
    }

    "an X defined elsewhere declines" {
        Grammar.abilityLine.parseLine("Counter target spell unless its controller pays {X}, where X is ~'s power.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
        Grammar.abilityLine.parseLine("Counter target spell unless its controller pays {2}{X}.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "the dynamic spelling of a fixed tax does not print" {
        val read = fragment("Counter target spell unless its controller pays {2}.")
        val dynamic = read.copy(
            script = read.script.copy(spellEffect = Effects.CounterUnlessDynamicPays(DynamicAmounts.fixed(2))),
        )
        Grammar.abilityLine.printLine(dynamic) shouldBe null
    }
})
