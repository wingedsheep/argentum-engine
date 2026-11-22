package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Horror of the Dim
 * {4}{B}
 * Creature — Horror
 * 3/4
 * {U}: This creature gains hexproof until end of turn.
 */
val HorrorOfTheDim = card("Horror of the Dim") {
    manaCost = "{4}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Horror"
    oracleText = "{U}: This creature gains hexproof until end of turn. (It can't be the target of spells or abilities your opponents control.)"
    power = 3
    toughness = 4

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.GrantKeyword(Keyword.HEXPROOF, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Jack Wang"
        flavorText = "\"Lesser minds can scarcely imagine such a marriage of necromancy and mind magic.\"\n—Strava, Dimir mage"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5d36c9d-967e-42dc-890c-0485b12f704f.jpg"
    }
}
