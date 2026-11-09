package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sibsig Icebreakers — Dragons of Tarkir #119 (canonical printing)
 * {2}{B} · Creature — Zombie · 2/3
 *
 * When this creature enters, each player discards a card.
 */
val SibsigIcebreakers = card("Sibsig Icebreakers") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, each player discards a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.EachPlayerDiscards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "119"
        artist = "Zoltan Boros"
        flavorText = "\"I almost envy them. They can't get frostbite.\"\n—Kirada, Silumgar enforcer"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/3835752e-11a6-406c-8bff-b7d4f2f31d85.jpg?1783938594"
    }
}
