package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Buy Your Silence
 * {4}{W}
 * Sorcery
 * Exile target nonland permanent. Its controller creates a Treasure token.
 *
 * `EffectTarget.TargetController` reads the exiled permanent's last-known controller, the same
 * shape as Zuko's Exile ("exile ..., its controller creates a Clue").
 */
val BuyYourSilence = card("Buy Your Silence") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Exile target nonland permanent. Its controller creates a Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        val t = target(TargetFilter.NonlandPermanent)
        effect = Effects.Exile(t) then
            Effects.CreateTreasure(controller = EffectTarget.TargetController)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Tony Foti"
        flavorText = "Mikey \"the Mouth\" kept his nickname after he struck his deal with Gregorio, even though he never spoke another word."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c9ac05f-f47c-4569-a06e-793d28cbb936.jpg"
    }
}
