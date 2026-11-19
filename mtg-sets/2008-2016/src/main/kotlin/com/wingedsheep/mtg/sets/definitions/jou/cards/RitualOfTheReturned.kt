package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ritual of the Returned
 * {3}{B}
 * Instant
 * Exile target creature card from your graveyard. Create a black Zombie creature token. Its power
 * is equal to that card's power and its toughness is equal to that card's toughness.
 *
 * Same shape as Soul Separator's Zombie half: the exiled card keeps its printed characteristics,
 * so its power/toughness are read off the target after the move.
 */
val RitualOfTheReturned = card("Ritual of the Returned") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile target creature card from your graveyard. Create a black Zombie creature token. " +
        "Its power is equal to that card's power and its toughness is equal to that card's toughness."

    spell {
        val graveyardCreature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Exile(graveyardCreature) then
            Effects.CreateDynamicToken(
                dynamicPower = DynamicAmounts.powerOf(graveyardCreature),
                dynamicToughness = DynamicAmounts.toughnessOf(graveyardCreature),
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie")
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Zack Stella"
        flavorText = "A mask forged to avenge a death forgotten."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20bdf00a-e7f1-42f4-997f-388f3b28bf8f.jpg?1783939431"
    }
}
