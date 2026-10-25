package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Ranger's Hawk
 * {W}
 * Creature — Bird
 * 1/1
 * Flying
 * {3}, {T}, Tap another untapped creature you control: Venture into the dungeon. Activate only as a sorcery.
 */
val RangersHawk = card("Ranger's Hawk") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "{3}, {T}, Tap another untapped creature you control: Venture into the dungeon. Activate only as a sorcery. (Enter the first room or advance to the next room.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.TapAnotherPermanent(GameObjectFilter.Creature)
        )
        effect = Effects.VentureIntoTheDungeon()
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "37"
        artist = "Robin Olausson"
        flavorText = "Swift eyes to scout the way ahead."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d7a7744-e49d-4b4e-b6ae-7fc6d2892cef.jpg?1783926525"
    }
}
