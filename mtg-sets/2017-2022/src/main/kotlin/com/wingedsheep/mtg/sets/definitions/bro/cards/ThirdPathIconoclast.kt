package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Third Path Iconoclast
 * {U}{R}
 * Creature — Human Monk
 * 2/1
 * Whenever you cast a noncreature spell, create a 1/1 colorless Soldier artifact creature token.
 */
val ThirdPathIconoclast = card("Third Path Iconoclast") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Monk"
    power = 2
    toughness = 1
    oracleText = "Whenever you cast a noncreature spell, create a 1/1 colorless Soldier artifact creature token."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Soldier"),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Manuel Castañón"
        flavorText = "\"When all you have is a hammer, everything looks like a nail. I merely propose expanding our toolbox.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1a21287-e244-4960-84fb-c4f6e5c346d9.jpg"
    }
}
