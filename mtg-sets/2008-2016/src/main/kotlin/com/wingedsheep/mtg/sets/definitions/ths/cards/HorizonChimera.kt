package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Horizon Chimera
 * {2}{G}{U}
 * Creature — Chimera
 * 3/2
 *
 * Flash
 * Flying, trample
 * Whenever you draw a card, you gain 1 life.
 */
val HorizonChimera = card("Horizon Chimera") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Chimera"
    oracleText = "Flash\nFlying, trample\nWhenever you draw a card, you gain 1 life."
    power = 3
    toughness = 2

    keywords(Keyword.FLASH, Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.GainLife(1)
        description = "Whenever you draw a card, you gain 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73b0e296-d556-4e76-bb2e-5a640e070bb5.jpg?1783939730"
    }
}
