package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dutiful Thrull
 * {W}
 * Creature — Thrull
 * 1/1
 * {B}: Regenerate this creature.
 */
val DutifulThrull = card("Dutiful Thrull") {
    manaCost = "{W}"
    colorIdentity = "WB"
    typeLine = "Creature — Thrull"
    oracleText = "{B}: Regenerate this creature."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "11"
        artist = "Daarken"
        flavorText = "\"They are useful, but there's little satisfaction in supplication that throws itself under your heel.\"\n—Jozica, Orzhov enforcer"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d586143-fac0-463f-96ec-c6b9fd582194.jpg"
    }
}
