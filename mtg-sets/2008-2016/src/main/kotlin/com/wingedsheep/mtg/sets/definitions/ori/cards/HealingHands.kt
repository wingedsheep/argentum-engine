package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Healing Hands
 * {2}{W}
 * Sorcery
 * Target player gains 4 life.
 * Draw a card.
 */
val HealingHands = card("Healing Hands") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Target player gains 4 life.\nDraw a card."

    spell {
        val player = target(Targets.Player)
        effect = Effects.GainLife(4, player) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Josu Hernaiz"
        flavorText = "The ethereal touch brings relief. The physical touch brings peace."
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a26559c-0ac5-42a7-b6ac-b39938c31026.jpg?1783938362"
    }
}
