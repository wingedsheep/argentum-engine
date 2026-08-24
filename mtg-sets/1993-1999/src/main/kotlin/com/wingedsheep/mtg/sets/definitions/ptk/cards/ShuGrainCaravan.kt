package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Shu Grain Caravan
 * {2}{W}
 * Creature — Human Soldier
 * 2/2
 * When this creature enters, you gain 2 life.
 */
val ShuGrainCaravan = card("Shu Grain Caravan") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, you gain 2 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "26"
        artist = "Li Wang"
        flavorText = "Keeping a million-man army fed was no easy task. Grain and rice caravans were the lifeblood of the empire."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bf26eb7-8a31-4022-87bb-67394653f06a.jpg"
    }
}
