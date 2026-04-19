package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeywordToCreatureGroup
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Boldwyr Aggressor
 * {3}{R}{R}
 * Creature — Giant Warrior
 * 2/5
 *
 * Double strike
 * Other Giants you control have double strike.
 */
val BoldwyrAggressor = card("Boldwyr Aggressor") {
    manaCost = "{3}{R}{R}"
    typeLine = "Creature — Giant Warrior"
    power = 2
    toughness = 5
    oracleText = "Double strike\nOther Giants you control have double strike."

    keywords(Keyword.DOUBLE_STRIKE)

    staticAbility {
        ability = GrantKeywordToCreatureGroup(
            keyword = Keyword.DOUBLE_STRIKE,
            filter = GroupFilter.AllCreaturesYouControl.withSubtype(Subtype.GIANT).other()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Aaron Miller"
        flavorText = "During the Invasion, Clan Boldwyr's defense of their dolmens was legendary. Now they're more territorial than ever."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76bbccd2-8a90-49a7-921a-6565b495efdf.jpg?1767871989"
    }
}
