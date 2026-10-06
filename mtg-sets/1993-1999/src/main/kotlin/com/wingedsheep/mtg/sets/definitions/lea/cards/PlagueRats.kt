package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Plague Rats
 * {2}{B}
 * Creature — Rat
 * * / *
 * Plague Rats's power and toughness are each equal to the number of creatures named Plague Rats on the battlefield.
 */
val PlagueRats = card("Plague Rats") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Rat"
    oracleText = "Plague Rats's power and toughness are each equal to the number of creatures named Plague Rats on the battlefield."
    colorIdentity = "B"
    dynamicStats(DynamicAmounts.battlefield(
        Player.Each, GameObjectFilter.Creature.named("Plague Rats")
    ).count())
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "121"
        artist = "Anson Maddocks"
        flavorText = "\"Should you a Rat to madness tease\nWhy ev'n a Rat may plague you...\"\n—Samuel Coleridge, \"Recantation\""
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3724e40-0622-4aee-9334-6c9fff88bcd5.jpg?1783948692"
    }
}
