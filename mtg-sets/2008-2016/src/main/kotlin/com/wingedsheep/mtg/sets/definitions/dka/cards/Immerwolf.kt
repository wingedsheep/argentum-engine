package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Immerwolf
 * {1}{R}{G}
 * Creature — Wolf
 * 2/2
 *
 * Intimidate
 * Each other creature you control that's a Wolf or a Werewolf gets +1/+1.
 * Non-Human Werewolves you control can't transform.
 *
 * The transform lock is the [AbilityFlag.CANT_TRANSFORM] flag (as on Bound by Moonsilver) granted
 * to every non-Human Werewolf permanent you control. The engine honors it in the single shared
 * transform-in-place implementation, so it stops both the classic Innistrad werewolf upkeep
 * triggers and daybound/nightbound day-change flips. A front-face Human Werewolf is still free to
 * transform; once it's a non-Human Werewolf it's locked.
 */
val Immerwolf = card("Immerwolf") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Wolf"
    power = 2
    toughness = 2
    oracleText = "Intimidate (This creature can't be blocked except by artifact creatures and/or " +
        "creatures that share a color with it.)\n" +
        "Each other creature you control that's a Wolf or a Werewolf gets +1/+1.\n" +
        "Non-Human Werewolves you control can't transform."

    keywords(Keyword.INTIMIDATE)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature
                    .withAnyOfSubtypes(listOf(Subtype.WOLF, Subtype.WEREWOLF))
                    .youControl(),
                excludeSelf = true,
            ),
        )
    }

    staticAbility {
        ability = GrantKeyword(
            AbilityFlag.CANT_TRANSFORM.name,
            GroupFilter(
                GameObjectFilter.Permanent
                    .withSubtype(Subtype.WEREWOLF)
                    .notSubtype(Subtype.HUMAN)
                    .youControl(),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "141"
        artist = "Terese Nielsen"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/9326061f-ea76-4be7-a06f-aefb63454777.jpg?1783940797"
        ruling("2011-01-22", "A creature that's both a Wolf and a Werewolf gets the bonus only once.")
        ruling(
            "2011-01-22",
            "Immerwolf's bonus never applies to itself, even if it somehow becomes a Werewolf creature.",
        )
        ruling("2011-01-22", "A \"non-Human Werewolf\" is a creature that is a Werewolf but is not a Human.")
    }
}
