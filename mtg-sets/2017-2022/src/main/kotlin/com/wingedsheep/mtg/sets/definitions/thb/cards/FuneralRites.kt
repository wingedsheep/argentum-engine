package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Funeral Rites
 * {2}{B}
 * Sorcery
 * You draw two cards, lose 2 life, then mill two cards.
 *
 * Three steps in printed order: draw two, lose 2 life, mill two.
 */
val FuneralRites = card("Funeral Rites") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "You draw two cards, lose 2 life, then mill two cards."

    spell {
        effect = Effects.DrawCards(2) then
            Effects.LoseLife(2, EffectTarget.Controller) then
            Patterns.Library.mill(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "John Stanko"
        flavorText = "Funerary masks help the newly dead resist the erosion of memory and identity in the Underworld."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30aa6796-0cb6-426b-9e3f-4d7e4959a6a4.jpg?1783931567"
    }
}
