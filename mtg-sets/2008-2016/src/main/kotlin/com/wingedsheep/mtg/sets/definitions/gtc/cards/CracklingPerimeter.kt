package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crackling Perimeter
 * {1}{R}
 * Enchantment
 * Tap an untapped Gate you control: This enchantment deals 1 damage to each opponent.
 *
 * The Gate cost is Gateway Shade's [Costs.TapPermanents] over Gate lands.
 */
val CracklingPerimeter = card("Crackling Perimeter") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Tap an untapped Gate you control: This enchantment deals 1 damage to each opponent."

    activatedAbility {
        cost = Costs.TapPermanents(1, GameObjectFilter.Land.withSubtype("Gate"))
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Tap an untapped Gate you control: This enchantment deals 1 damage to each opponent."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Yeong-Hao Han"
        flavorText = "As the path he sought came into focus, Niv-Mizzet took steps to prevent others from approaching it."
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3323c86c-73bd-4e23-9f80-54bf5c1dd0bc.jpg?1783940127"
    }
}
