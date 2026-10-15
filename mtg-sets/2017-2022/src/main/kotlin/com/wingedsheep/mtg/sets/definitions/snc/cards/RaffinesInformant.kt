package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Raffine's Informant
 * {1}{W}
 * Creature — Human Wizard
 * 2 / 1
 * When this creature enters, it connives.
 */
val RaffinesInformant = card("Raffine's Informant") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature enters, it connives. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Connive()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "John Stanko"
        flavorText = "\"I've analyzed the vault schematic. Do you want the fast way, the easy way, or the secret way in?\""
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4e64ff87-2099-4360-94f6-164277b7b514.jpg"
    }
}
