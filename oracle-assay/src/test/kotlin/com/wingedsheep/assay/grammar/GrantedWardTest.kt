package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * A granted ward is the last member of a grant clause's keyword run, and the SDK grants it as a
 * [GrantWard] static after one [GrantKeyword] per plain keyword — in every grant position the
 * keyword-only run reaches: the attachment, the attachment with a pump, a group, and a group with a
 * pump. The negative cases pin the reconstruct-and-compare: an order, filter or cost the sentence
 * cannot say refuses to print rather than printing a sentence that drops it.
 */
class GrantedWardTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun printsNothing(vararg statics: com.wingedsheep.sdk.scripting.StaticAbility) {
        Grammar.abilityLine.printLine(CardFragment(script = CardScript(staticAbilities = statics.toList()))) shouldBe null
    }

    val ward1 = WardCost.Mana("{1}")
    val ward2 = WardCost.Mana("{2}")

    // Chains of Custody, Secret Invasion.
    "the attachment's ward alone is one GrantWard over the attached creature" {
        fragment("Enchanted creature has ward {2}.").script.staticAbilities shouldBe listOf(GrantWard(ward2))
        roundTrips("Enchanted creature has ward {2}.")
    }

    // Crystal Carapace, Lavaspur Boots, Sheltered by Ghosts.
    "a pump and a run ending in ward is the pump, the keyword grants, then the ward" {
        fragment("Enchanted creature gets +3/+3 and has ward {2}.").script.staticAbilities shouldBe
            listOf(ModifyStats(3, 3), GrantWard(ward2))
        fragment("Enchanted creature gets +1/+0 and has haste and ward {1}.").script.staticAbilities shouldBe
            listOf(ModifyStats(1, 0), GrantKeyword(Keyword.HASTE), GrantWard(ward1))
        roundTrips("Enchanted creature gets +3/+3 and has ward {2}.")
        roundTrips("Enchanted creature gets +1/+0 and has haste and ward {1}.")
        roundTrips("Enchanted creature gets +1/+1 and has flying, vigilance, and ward {1}.")
        roundTrips("Enchanted creature has lifelink and ward {2}.")
    }

    // Star Whale; Flowering of the White Tree's shape over a group the filter vocabulary reads.
    "a group's ward carries the group's filter on every grant" {
        val group = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true)
        fragment("Other creatures you control have ward {2}.").script.staticAbilities shouldBe
            listOf(GrantWard(ward2, group))
        roundTrips("Other creatures you control have ward {2}.")
        roundTrips("Creatures you control have deathtouch and ward {2}.")
        roundTrips("Creatures you control get +2/+1 and have ward {1}.")
    }

    "an order, a filter or a cost the sentence cannot say refuses to print" {
        // Ward printed before the keyword it follows.
        printsNothing(GrantWard(ward1), GrantKeyword(Keyword.HASTE))
        // A ward over a different set than the pump beside it.
        printsNothing(ModifyStats(1, 1), GrantWard(ward1, GroupFilter(GameObjectFilter.Creature.youControl())))
        // The em-dash costs are quoted sentences when granted, not a run member.
        printsNothing(GrantWard(WardCost.Life(2)))
        printsNothing(GrantWard(WardCost.Mana("{1}", waterbend = true)))
    }

    "a granted ward is a static grant and not an effect grant" {
        Grammar.abilityLine.parseLine("Target creature gains ward {2} until end of turn.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }
})
