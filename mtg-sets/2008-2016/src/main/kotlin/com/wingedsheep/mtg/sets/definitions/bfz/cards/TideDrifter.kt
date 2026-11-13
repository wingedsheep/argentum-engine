package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Tide Drifter
 * {1}{U}
 * Creature — Eldrazi Drone
 * 0/5
 * Devoid (This card has no color.)
 * Other colorless creatures you control get +0/+1.
 *
 * Same lord shape as Ruination Guide: the filter reads projected colour, so devoid creatures and
 * colorless artifact creatures both get the bonus.
 */
val TideDrifter = card("Tide Drifter") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 0
    toughness = 5
    oracleText = "Devoid (This card has no color.)\n" +
        "Other colorless creatures you control get +0/+1."

    keywords(Keyword.DEVOID)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 0,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "67"
        artist = "Daarken"
        flavorText = "It didn't do much, but the residents of Sea Gate gave it a wide berth all the same."
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c590579-e0ef-4839-8f67-f52813cd9fe7.jpg?1783938211"
    }
}
