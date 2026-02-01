package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.WheelEffect

/**
 * Winds of Change
 * {R}
 * Sorcery
 * Each player shuffles the cards from their hand into their library, then draws that many cards.
 */
val WindsOfChange = card("Winds of Change") {
    manaCost = "{R}"
    typeLine = "Sorcery"

    spell {
        effect = WheelEffect(target = EffectTarget.EachPlayer)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "NéNé Thomas"
        flavorText = "Change comes like the wind—swift and unpredictable."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/735b8aec-62d4-46db-9a68-a6c69cb6fd98.jpg"
    }
}
