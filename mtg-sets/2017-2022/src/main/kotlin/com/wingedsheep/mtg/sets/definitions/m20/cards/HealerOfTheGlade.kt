package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Healer of the Glade (M20 #176)
 * {G}  Creature — Elemental  1/2
 *
 * When this creature enters, you gain 3 life.
 */
val HealerOfTheGlade = card("Healer of the Glade") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    oracleText = "When this creature enters, you gain 3 life."
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Daarken"
        flavorText = "\"Grief and rage and nightmares fade\nWhere hope and comfort make their stand.\"\n—*Lament for Bala Ged*, stanza 4"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbe262f2-e35b-4c85-938d-3e9e9c764c1b.jpg"
    }
}
