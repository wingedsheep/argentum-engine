package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Gravity Sphere
 * {2}{R}
 * World Enchantment
 *
 * All creatures lose flying.
 */
val GravitySphere = card("Gravity Sphere") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "World Enchantment"
    oracleText = "All creatures lose flying."

    staticAbility {
        ability = RemoveKeywordStatic(Keyword.FLYING, GroupFilter(GameObjectFilter.Creature))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "151"
        artist = "Brian Snõddy"
        flavorText = "On the morning of the Battle of Gal-Shan, the Sorcerer's cry rang out: \"Let no bird fly, let no creature take wing, let all the battle join as one.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2749332-e99a-4a0c-b3a3-5578b552fa11.jpg?1783948056"
    }
}
