package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wakedancer
 * {2}{B}
 * Creature — Human Shaman
 * 2/2
 * Morbid — When this creature enters, if a creature died this turn, create a 2/2 black Zombie creature token.
 */
val Wakedancer = card("Wakedancer") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Shaman"
    oracleText = "Morbid — When this creature enters, if a creature died this turn, create a 2/2 black Zombie creature token."
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/1/7/17f001ab-514b-49e7-a657-b2872ad7a1de.jpg?1767954964",
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Austin Hsu"
        flavorText = "Hers is an ancient form of necromancy, steeped in shamanic trance and ritual that few skaberen or ghoulcallers comprehend."
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f533fbfa-42ae-4e27-92a4-9936bcd2a5f4.jpg"
    }
}
