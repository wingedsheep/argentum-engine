package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Downsize — Return to Ravnica #38 (canonical printing)
 * {U} · Instant
 *
 * Target creature you don't control gets -4/-0 until end of turn.
 * Overload {2}{U}
 *
 * Blustersquall's "you don't control" filter with Weapon Surge's stat-modifying overload shape.
 */
val Downsize = card("Downsize") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature you don't control gets -4/-0 until end of turn.\n" +
        "Overload {2}{U} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{2}{U}"))

    spell {
        val notYours = GameObjectFilter.Creature.withControllerPredicate(
            ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
        )
        val creature = target(TargetFilter(notYours))
        effect = Effects.ModifyStats(-4, 0, creature)

        overloadEffect = Patterns.Group.modifyStatsForAll(-4, 0, GroupFilter(notYours))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e8408a52-d34a-4e03-9312-700dec75d844.jpg?1783940370"
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
    }
}
