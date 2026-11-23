package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gateway Shade
 * {2}{B}
 * Creature — Shade
 * 1/1
 * {B}: This creature gets +1/+1 until end of turn.
 * Tap an untapped Gate you control: This creature gets +2/+2 until end of turn.
 *
 * The Shade pump is Evernight Shade's shape; the Gate pump pays [Costs.TapPermanents] over
 * Gate lands (Tradewind Rider's tap-untapped-permanents cost). Neither cost taps the Shade, so
 * both are usable while it is attacking or summoning-sick.
 */
val GatewayShade = card("Gateway Shade") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Shade"
    power = 1
    toughness = 1
    oracleText = "{B}: This creature gets +1/+1 until end of turn.\n" +
        "Tap an untapped Gate you control: This creature gets +2/+2 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.TapPermanents(1, GameObjectFilter.Land.withSubtype("Gate"))
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "Tap an untapped Gate you control: This creature gets +2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Ryan Yee"
        flavorText = "Tireless and merciless, shades are harnessed as spies by the Dimir."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa33fc15-3a4f-48bc-be7c-fdec1cb49c10.jpg?1783940131"
    }
}
