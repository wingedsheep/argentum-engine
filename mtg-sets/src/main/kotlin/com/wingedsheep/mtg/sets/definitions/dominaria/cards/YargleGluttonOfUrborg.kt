package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Yargle, Glutton of Urborg
 * {4}{B}
 * Legendary Creature — Frog Spirit
 * 9/3
 */
val YargleGluttonOfUrborg = card("Yargle, Glutton of Urborg") {
    manaCost = "{4}{B}"
    typeLine = "Legendary Creature — Frog Spirit"
    power = 9
    toughness = 3

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Jehan Choo"
        flavorText = "When Belzenlok's lieutenant Yar-Kul grew too ambitious, the Demonlord transformed him into a maggot. The frog that ate the maggot grew and grew, until a ravenous spirit burst from its body."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/645cfc1b-76f2-4823-9fb0-03cb009f8b32.jpg?1562736801"
    }
}
