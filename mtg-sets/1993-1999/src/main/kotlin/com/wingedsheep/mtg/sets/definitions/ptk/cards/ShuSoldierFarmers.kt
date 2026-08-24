package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Shu Soldier-Farmers
 * {4}{W}
 * Creature — Human Soldier
 * 2/4
 * When this creature enters, you gain 4 life.
 */
val ShuSoldierFarmers = card("Shu Soldier-Farmers") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, you gain 4 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.GainLife(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Li Xiaohua"
        flavorText = "During Kongming's campaigns against the Wei, his Shu troops rotated from the battlefront to the fields every hundred days."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edb576e9-98ba-4bd1-9d1e-e316acf2e7f5.jpg"
    }
}
