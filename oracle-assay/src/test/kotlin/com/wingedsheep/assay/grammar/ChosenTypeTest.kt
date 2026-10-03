package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The chosen-type band: "of the chosen type" is `GroupFilter.chosenSubtypeKey` on a plural lord and
 * `CardPredicate.HasChosenSubtype` on a singular noun — one phrase, two fields, split by position.
 */
class ChosenTypeTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    "chosen-type lines round-trip" {
        for (
            line in listOf(
                "Creatures you control of the chosen type get +1/+1.",
                "Other creatures you control of the chosen type get +1/+1.",
                "Creatures of the chosen type have fear.",
                "Creatures you control of the chosen type get +2/+2 and have first strike and trample.",
                "Whenever a permanent you control of the chosen type enters, you gain 1 life.",
            )
        ) {
            Grammar.abilityLine.printLine(fragment(line)) shouldBe line
        }
    }

    "a lord over the chosen type lands on the group's chosen-subtype key" {
        fragment("Creatures you control of the chosen type get +1/+1.").script.staticAbilities shouldBe
            listOf(ModifyStats(1, 1, GroupFilter.ChosenSubtypeCreatures().youControl()))
        fragment("Creatures of the chosen type have fear.").script.staticAbilities shouldBe
            listOf(GrantKeyword(Keyword.FEAR, GroupFilter.ChosenSubtypeCreatures()))
        fragment("Other creatures you control of the chosen type get +1/+1.").script.staticAbilities shouldBe
            listOf(ModifyStats(1, 1, GroupFilter.ChosenSubtypeCreatures(excludeSelf = true).youControl()))
    }

    "a lord over a pipeline's own stored choice does not print" {
        val stored = CardFragment.of(
            CardScript(
                staticAbilities = listOf(
                    ModifyStats(1, 1, GroupFilter.ChosenSubtypeCreatures(key = "elsewhere").youControl()),
                ),
            ),
        )
        Grammar.abilityLine.printLine(stored) shouldBe null
    }
})
