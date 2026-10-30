package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mystic Repeal
 * {G}
 * Instant
 * Put target enchantment on the bottom of its owner's library.
 */
val MysticRepeal = card("Mystic Repeal") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put target enchantment on the bottom of its owner's library."

    spell {
        val t = target(TargetFilter.Enchantment)
        effect = Effects.PutOnBottomOfLibrary(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Tyler Walpole"
        flavorText = "\"No power is too great to be undone.\"\n—Karametra, god of harvests"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6797195a-508f-45b1-964d-da842dc46ca8.jpg?1783931537"
    }
}
