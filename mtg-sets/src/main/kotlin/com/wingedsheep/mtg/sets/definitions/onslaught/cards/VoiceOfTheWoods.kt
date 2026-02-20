package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Voice of the Woods
 * {3}{G}{G}
 * Creature — Elf
 * 2/2
 * Tap five untapped Elves you control: Create a 7/7 green Elemental creature token with trample.
 */
val VoiceOfTheWoods = card("Voice of the Woods") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Elf"
    power = 2
    toughness = 2
    oracleText = "Tap five untapped Elves you control: Create a 7/7 green Elemental creature token with trample."

    activatedAbility {
        cost = Costs.TapPermanents(5, GameObjectFilter.Creature.withSubtype("Elf"))
        effect = CreateTokenEffect(
            power = 7,
            toughness = 7,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elemental"),
            keywords = setOf(Keyword.TRAMPLE)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "297"
        artist = "Pete Venters"
        flavorText = "The ritual of making draws upon the elves' memories and pasts. And elves have long memories and ancient pasts."
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ebb4668-eebf-4b7e-ae29-75fff5963868.jpg?1562902199"
    }
}
