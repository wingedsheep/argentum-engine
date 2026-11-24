package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Teleportal — Return to Ravnica #202 (canonical printing)
 * {U}{R} · Sorcery
 *
 * Target creature you control gets +1/+0 until end of turn and can't be blocked this turn.
 * Overload {3}{U}{R}
 *
 * Dynacharge's pump shape plus evasion. Overloaded, the +1/+0 is locked in at resolution
 * (`modifyStatsForAll`), while [Effects.CantBeBlockedGroup] keeps its group filter live — exactly
 * the 2017-03-14 ruling: any creature you control when blockers are chosen can't be blocked.
 */
val Teleportal = card("Teleportal") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Target creature you control gets +1/+0 until end of turn and can't be blocked this turn.\n" +
        "Overload {3}{U}{R} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{3}{U}{R}"))

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(1, 0, creature) then
            Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)

        overloadEffect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter.AllCreaturesYouControl) then
            Effects.CantBeBlockedGroup(GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e438c718-ac18-45d5-824e-5b697c5f0692.jpg?1783940330"
        ruling(
            "2024-01-12",
            "If you don't pay the overload cost of a spell with overload, that spell will have a single " +
                "target. If you pay the overload cost, the spell won't have any targets."
        )
        ruling(
            "2024-01-12",
            "Because a spell with overload doesn't target when its overload cost is paid, it may affect " +
                "permanents with hexproof or with protection from the appropriate color."
        )
        ruling(
            "2024-01-12",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying (such as an overload cost), add any cost increases, then apply any cost reductions. " +
                "The mana value of the spell remains unchanged, no matter what the total cost to cast it was."
        )
        ruling(
            "2024-01-12",
            "If you are instructed to cast a spell with overload \"without paying its mana cost,\" you can't " +
                "choose to pay its overload cost instead."
        )
        ruling(
            "2017-03-14",
            "The set of creatures that gets +1/+0 from an overloaded Teleportal is determined as the spell " +
                "resolves. Creatures you begin to control later in the turn won't get +1/+0. However, because " +
                "the second part of Teleportal's effect doesn't change the characteristics of any permanents, " +
                "the set of creatures that can't be blocked is constantly updated. Any creature you control at " +
                "the moment blockers are chosen can't be blocked."
        )
    }
}
