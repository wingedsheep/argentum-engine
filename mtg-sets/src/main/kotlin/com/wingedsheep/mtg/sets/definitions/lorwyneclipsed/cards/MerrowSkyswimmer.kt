package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Merrow Skyswimmer
 * {3}{W/U}{W/U}
 * Creature — Merfolk Soldier
 * 2/2
 *
 * Convoke
 * Flying, vigilance
 * When this creature enters, create a 1/1 white and blue Merfolk creature token.
 */
val MerrowSkyswimmer = card("Merrow Skyswimmer") {
    manaCost = "{3}{W/U}{W/U}"
    typeLine = "Creature — Merfolk Soldier"
    power = 2
    toughness = 2
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Flying, vigilance\n" +
        "When this creature enters, create a 1/1 white and blue Merfolk creature token."

    keywords(Keyword.CONVOKE, Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CreateTokenEffect(
            count = 1,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE, Color.BLUE),
            creatureTypes = setOf("Merfolk"),
            imageUri = "https://cards.scryfall.io/normal/front/4/c/4c5ad4e1-b489-4023-88ab-1200c5f26ffc.jpg?1767955704"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "234"
        artist = "Richard Kane Ferguson"
        flavorText = "\"Only fools believe us bound to water.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/7/075b419a-fd44-4a9a-8c40-474562b7e11a.jpg?1767872219"
    }
}
