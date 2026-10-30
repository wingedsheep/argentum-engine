package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bushmeat Poacher
 * {3}{B}
 * Creature — Human Soldier
 * 2/4
 * {1}, {T}, Sacrifice another creature: You gain life equal to the sacrificed creature's toughness.
 * Draw a card.
 *
 * The sacrificed creature's toughness is read from the cost snapshot — its last-known toughness on
 * the battlefield, per the ruling.
 */
val BushmeatPoacher = card("Bushmeat Poacher") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 4
    oracleText = "{1}, {T}, Sacrifice another creature: You gain life equal to the sacrificed creature's toughness. Draw a card."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = Effects.GainLife(DynamicAmounts.sacrificedToughness()) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Randy Vargas"
        flavorText = "\"If you've never feasted on Zagoth terror-pig, I got the best price you'll find.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc01faf4-fabb-40c9-a1d6-359280535f91.jpg?1783931065"
        ruling("2020-04-17", "Use the sacrificed creature's toughness as it last existed on the battlefield to determine how much life you gain.")
        ruling("2020-04-17", "You draw only one card, no matter how much life you gain.")
    }
}
