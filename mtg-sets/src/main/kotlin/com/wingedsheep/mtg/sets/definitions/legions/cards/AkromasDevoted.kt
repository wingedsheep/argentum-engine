package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Akroma's Devoted
 * {3}{W}
 * Creature — Human Cleric
 * 2/4
 * Cleric creatures have vigilance.
 */
val AkromasDevoted = card("Akroma's Devoted") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 4
    oracleText = "Cleric creatures have vigilance."

    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.VIGILANCE,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Cleric"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "Dave Dorman"
        flavorText = "Akroma asked for only one thing from her troops: unwavering, unconditional loyalty."
        imageUri = "https://cards.scryfall.io/normal/front/7/9/798893df-e720-471d-822d-50284de23efd.jpg?1562919405"
    }
}
