package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eliminate
 * {1}{B}
 * Instant
 *
 * Destroy target creature or planeswalker with mana value 3 or less.
 */
val Eliminate = card("Eliminate") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target creature or planeswalker with mana value 3 or less."

    spell {
        val t = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.manaValueAtMost(3)))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Chris Cold"
        flavorText = "\"Cruelty? No. This is necessity.\"\n—Kaervek"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8eb4087-3a4c-4de8-8e29-f4cd71acb180.jpg?1783930710"
        ruling("2020-06-23", "The mana value of a token that isn't a copy of another object is 0. A token that is a copy of another object has the same mana cost as that object.")
        ruling("2020-06-23", "If a creature or planeswalker on the battlefield has {X} in its mana cost, X is considered to be 0.")
    }
}
