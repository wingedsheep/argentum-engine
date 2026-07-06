package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Earthshaker Dreadmaw — {4}{G}{G}
 * Creature — Dinosaur
 * 6/6
 * Trample
 * When this creature enters, draw a card for each other Dinosaur you control.
 */
val EarthshakerDreadmaw = card("Earthshaker Dreadmaw") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    oracleText = "Trample\nWhen this creature enters, draw a card for each other Dinosaur you control."
    power = 6
    toughness = 6

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(
            DynamicAmount.AggregateBattlefield(
                player = Player.You,
                filter = GameObjectFilter.Creature.withSubtype("Dinosaur"),
                aggregation = Aggregation.COUNT,
                excludeSelf = true,
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "183"
        artist = "Jesper Ejsing"
        flavorText = "\"I suggest we leave this cavern for someone else to conquer.\" —Amalia, master cartographer"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdcbba6f-aa54-44be-a3b0-f712fa8bd5ad.jpg?1782694463"
    }
}
