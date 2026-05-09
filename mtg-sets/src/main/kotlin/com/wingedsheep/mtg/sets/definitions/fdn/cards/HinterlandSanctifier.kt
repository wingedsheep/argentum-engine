package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hinterland Sanctifier
 * {W}
 * Creature — Rabbit Cleric
 * 1/2
 *
 * Whenever another creature you control enters, you gain 1 life.
 */
val HinterlandSanctifier = card("Hinterland Sanctifier") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Rabbit Cleric"
    power = 1
    toughness = 2
    oracleText = "Whenever another creature you control enters, you gain 1 life."

    // Whenever another creature you control enters, you gain 1 life
    triggeredAbility {
        trigger = Triggers.OtherCreatureEnters
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "730"
        artist = "Justine Cruz"
        imageUri = "https://cards.scryfall.io/normal/front/6/3/632df69e-6377-43d0-bba5-65518a320aa5.jpg?1751043344"
        flavorText = "\"You can learn a lot about a place just by asking the land how it's doing.\""
    }
}
