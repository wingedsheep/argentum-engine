package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Koilos Roc
 * {4}{U}
 * Creature — Bird
 * 3/3
 * Flash
 * Flying
 * When this creature enters, create a tapped Powerstone token.
 */
val KoilosRoc = card("Koilos Roc") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 3
    toughness = 3
    oracleText = "Flash\nFlying\nWhen this creature enters, create a tapped Powerstone token. (It's an artifact with \"{T}: Add {C}. This mana can't be spent to cast a nonartifact spell.\")"

    keywords(Keyword.FLASH, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreatePowerstone(tapped = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebdbcb87-c6ea-477f-a560-c175d99e21b8.jpg?1783920113"
    }
}
