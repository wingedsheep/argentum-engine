package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Elturgard Ranger
 * {4}{G}
 * Creature — Human Elf Ranger
 * 4/1
 * Reach
 * When this creature enters, create a 2/2 green Wolf creature token.
 */
val ElturgardRanger = card("Elturgard Ranger") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Elf Ranger"
    oracleText = "Reach\nWhen this creature enters, create a 2/2 green Wolf creature token."
    power = 4
    toughness = 1
    keywords(Keyword.REACH)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf")
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Francisco Miyara"
        flavorText = "Since the fall of Elturel, they've walked the Sword Coast from Tethyr to Icewind Dale, eyes always on their prey."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6898737-957f-44a6-bef7-fb196658176f.jpg?1783926464"
    }
}
