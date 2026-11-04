package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Angrath's Ambusher
 * {2}{B}
 * Creature — Orc Pirate
 * 2/3
 * This creature gets +2/+0 as long as you control an Angrath planeswalker.
 *
 * The condition is [Exists] over a battlefield planeswalker with the Angrath subtype —
 * [Player.You] carries the "you control" half, so the filter itself has no controller predicate.
 * `ModifyStats.filter` defaults to the attached creature, so [Filters.Self] is passed explicitly.
 */
val AngrathsAmbusher = card("Angrath's Ambusher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Orc Pirate"
    oracleText = "This creature gets +2/+0 as long as you control an Angrath planeswalker."
    power = 2
    toughness = 3

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 0, Filters.Self),
            condition = Exists(
                Player.You,
                Zone.BATTLEFIELD,
                GameObjectFilter.Planeswalker.withSubtype("Angrath")
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "202"
        inBooster = false
        artist = "David Palumbo"
        flavorText = "Orcs have been enemies of the Legion of Dusk since it was formed, and the " +
            "entire race has been declared anathema."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48181263-11fd-444e-9fef-02ccfe016c8c.jpg?1783935257"
    }
}
