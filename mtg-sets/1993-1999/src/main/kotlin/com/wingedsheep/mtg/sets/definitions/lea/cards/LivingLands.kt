package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AnimateLandGroup
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Living Lands — Limited Edition Alpha #209
 * {3}{G} · Enchantment
 *
 * All Forests are 1/1 creatures that are still lands.
 *
 * Ambush Commander's group animation without the controller restriction, colour, or creature
 * subtype: every Forest on the battlefield (either player's) gains the creature type and base
 * P/T 1/1, keeping its land types and colourlessness.
 */
val LivingLands = card("Living Lands") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "All Forests are 1/1 creatures that are still lands."

    staticAbility {
        ability = AnimateLandGroup(
            filter = GroupFilter(GameObjectFilter.Land.withSubtype("Forest")),
            power = 1,
            toughness = 1
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/80be0580-7948-4d8e-8c0f-5e2797ac411b.jpg?1783948674"
        ruling(
            "2008-08-01",
            "A noncreature permanent that turns into a creature can attack, and its {T} abilities can be " +
                "activated, only if its controller has continuously controlled that permanent since the " +
                "beginning of their most recent turn. It doesn't matter how long the permanent has been a creature."
        )
    }
}
