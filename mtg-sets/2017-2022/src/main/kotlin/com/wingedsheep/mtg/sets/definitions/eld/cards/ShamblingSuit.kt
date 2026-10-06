package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

val ShamblingSuit = card("Shambling Suit") {
    manaCost = "{3}"
    typeLine = "Artifact Creature — Construct"
    toughness = 3
    oracleText = "Shambling Suit's power is equal to the number of artifacts and/or enchantments you control."

    dynamicPower(DynamicAmounts.battlefield(Player.You, GameObjectFilter.ArtifactOrEnchantment).count())

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Nicholas Gregory"
        flavorText = "\"The young squire gripped her sword as the clanking stranger emerged. It was no knight that had come to challenge her.\"\n—*Beyond the Great Henge*"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/1100b898-31a8-4fdf-a54f-a1470ec032f3.jpg?1783932584"
        ruling("2019-10-04", "The ability that defines Shambling Suit's power works in all zones, not just the battlefield.")
        ruling("2019-10-04", "A permanent that's both an artifact and an enchantment is only counted once.")
    }
}
