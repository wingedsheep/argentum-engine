package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Inexorable Tide
 * {3}{U}{U}
 * Enchantment
 *
 * Whenever you cast a spell, proliferate.
 */
val InexorableTide = card("Inexorable Tide") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast a spell, proliferate. (Choose any number of permanents and/or " +
        "players, then give each another counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Dave Kendall"
        flavorText = "\"See how gratefully this world accepts our blessings.\"\n—Jin-Gitaxias, Core Augur"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f41e281-fcbb-450b-8a67-7b072c55c6f0.jpg?1783941739"
    }
}
