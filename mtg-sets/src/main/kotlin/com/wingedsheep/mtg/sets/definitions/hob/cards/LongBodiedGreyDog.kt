package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Long-Bodied Grey Dog
 * {3}
 * Creature — Dog
 * 2/2
 * Flash
 * Reach
 * When this creature enters, create a tapped Treasure token.
 */
val LongBodiedGreyDog = card("Long-Bodied Grey Dog") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Creature — Dog"
    oracleText = "Flash\nReach\nWhen this creature enters, create a tapped Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"
    power = 2
    toughness = 2
    keywords(Keyword.FLASH, Keyword.REACH)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateTreasure(tapped = true)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Anna Podedworna"
        flavorText = "Beorn's dogs took bowls and platters and knives and wooden spoons and quickly laid them on the trestle tables."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1a1e520-1fe2-4529-8afb-c187bb80da3c.jpg?1785639260"
    }
}
