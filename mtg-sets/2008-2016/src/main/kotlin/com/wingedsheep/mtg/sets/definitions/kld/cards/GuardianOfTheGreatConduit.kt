package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Guardian of the Great Conduit
 * {3}{G}
 * Creature — Elemental
 * 2/4
 * Reach (This creature can block creatures with flying.)
 * As long as you control a Nissa planeswalker, this creature gets +2/+0 and has vigilance.
 * (Attacking doesn't cause it to tap.)
 *
 * The printed "and" is two [ConditionalStaticAbility] wrappers over the same [Exists] check, so
 * each continuous effect lands in its own layer (7c for the pump, 6 for vigilance) while sharing
 * one condition that is re-evaluated continuously.
 */
val GuardianOfTheGreatConduit = card("Guardian of the Great Conduit") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "As long as you control a Nissa planeswalker, this creature gets +2/+0 and has vigilance. (Attacking doesn't cause it to tap.)"
    power = 2
    toughness = 4
    keywords(Keyword.REACH)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 0, Filters.Self),
            condition = Exists(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Planeswalker.withSubtype("Nissa")
            )
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, Filters.Self),
            condition = Exists(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Planeswalker.withSubtype("Nissa")
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "271"
        artist = "Christine Choi"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72638ac5-84fd-4688-9b81-0eea3c05e53e.jpg?1783937136"
        inBooster = false
    }
}
