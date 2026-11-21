package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Weapon Surge
 * {R}
 * Instant
 * Target creature you control gets +1/+0 and gains first strike until end of turn.
 * Overload {1}{R}
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val WeaponSurge = card("Weapon Surge") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +1/+0 and gains first strike until end of turn.\n" +
        "Overload {1}{R} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{1}{R}"))

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(1, 0, creature) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)

        overloadEffect = Patterns.Group.modifyStatsForAll(1, 0, Filters.Group.creaturesYouControl) then
            Patterns.Group.grantKeywordToAll(Keyword.FIRST_STRIKE, Filters.Group.creaturesYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f28df164-8bff-4428-b7dd-2974c288f1d3.jpg?1783940036"
        ruling(
            "2024-01-12",
            "Because a spell with overload doesn't target when its overload cost is paid, it may affect " +
                "permanents with hexproof or with protection from the appropriate color."
        )
        ruling(
            "2024-01-12",
            "If you don't pay the overload cost of a spell with overload, that spell will have a single " +
                "target. If you pay the overload cost, the spell won't have any targets."
        )
        ruling(
            "2024-01-12",
            "To determine the total cost of a spell, start with the mana cost or alternative cost you're " +
                "paying (such as an overload cost), add any cost increases, then apply any cost reductions. " +
                "The mana value of the spell remains unchanged, no matter what the total cost to cast it was."
        )
        ruling(
            "2024-01-12",
            "If you are instructed to cast a spell with overload \"without paying its mana cost,\" you " +
                "can't choose to pay its overload cost instead."
        )
    }
}
