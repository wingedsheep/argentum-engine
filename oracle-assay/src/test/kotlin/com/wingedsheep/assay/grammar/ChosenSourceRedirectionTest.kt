package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.scripting.effects.RedirectDamageFromChosenSourceEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ChosenSourceRedirectionTest : StringSpec({
    "Beacon's source choice survives parsing and printing" {
        val line = "The next time a source of your choice would deal damage to you this turn, that damage is dealt to ~ instead."
        val fragment = Grammar.abilityLine.parseLine(line)
            .shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value
        fragment.script.spellEffect shouldBe RedirectDamageFromChosenSourceEffect(
            protectedTarget = EffectTarget.Controller,
            redirectTo = EffectTarget.Self,
        )
        Grammar.abilityLine.printLine(fragment) shouldBe line
    }
})
