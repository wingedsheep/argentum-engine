package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mirrormere Guardian
 * {2}{G}
 * Creature — Dwarf Soldier
 * 4/2
 *
 * When this creature dies, the Ring tempts you.
 */
val MirrormereGuardian = card("Mirrormere Guardian") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dwarf Soldier"
    power = 4
    toughness = 2
    oracleText = "When this creature dies, the Ring tempts you."

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.TheRingTemptsYou()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Anton Solovianchyk"
        flavorText = "\"But still the sunken stars appear\nIn dark and windless Mirrormere;\nThere lies his crown in water deep,\nTill Durin wakes again from sleep.\"\n—Gimli's song"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/8315d7cc-fc2c-45b1-8340-11ff2af3beb5.jpg?1686969503"
    }
}
