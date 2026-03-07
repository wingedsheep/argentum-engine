package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.YouAttackedThisTurn

/**
 * Mardu Warshrieker
 * {3}{R}
 * Creature — Orc Shaman
 * 3/3
 * Raid — When this creature enters, if you attacked this turn, add {R}{W}{B}.
 */
val MarduWarshrieker = card("Mardu Warshrieker") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Orc Shaman"
    power = 3
    toughness = 3
    oracleText = "Raid — When this creature enters, if you attacked this turn, add {R}{W}{B}."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = YouAttackedThisTurn
        effect = Effects.Composite(
            Effects.AddMana(Color.RED),
            Effects.AddMana(Color.WHITE),
            Effects.AddMana(Color.BLACK)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Yefim Kligerman"
        flavorText = "\"No body can contain so much fury. It reminds me of another battle, long past.\"\n—Sarkhan Vol"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/1381ae25-0d44-4d06-beae-48e4bbb4bb45.jpg?1562782862"
    }
}
