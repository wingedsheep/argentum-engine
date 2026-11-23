package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dynacharge — Return to Ravnica #92 (canonical printing)
 * {R} · Instant
 *
 * Target creature you control gets +2/+0 until end of turn.
 * Overload {2}{R}
 *
 * Weapon Surge / Downsize's stat-modifying overload shape over creatures you control.
 */
val Dynacharge = card("Dynacharge") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +2/+0 until end of turn.\n" +
        "Overload {2}{R} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{2}{R}"))

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(2, 0, creature)

        overloadEffect = Patterns.Group.modifyStatsForAll(2, 0, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e612a032-39be-44cd-a78c-29f89dc384b0.jpg?1783940356"
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
            "An overloaded Dynacharge affects only creatures you control at the time it resolves. " +
                "Creatures you begin to control later in the turn won't get +2/+0."
        )
    }
}
