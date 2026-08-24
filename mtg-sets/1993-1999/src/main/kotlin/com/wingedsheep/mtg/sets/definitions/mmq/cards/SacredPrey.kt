package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sacred Prey
 * {G}
 * Creature — Horse
 * 1 / 1
 *
 * Whenever this creature becomes blocked, you gain 1 life.
 */
val SacredPrey = card("Sacred Prey") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Horse"
    oracleText = "Whenever this creature becomes blocked, you gain 1 life."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "268"
        artist = "Rebecca Guay"
        flavorText = "To see one is a good omen to the Cho-Arrim."
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e965d32c-3151-48e8-b256-0b7fa8a8a211.jpg"
    }
}
