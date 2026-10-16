package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Glimmer of Genius
 * {3}{U}
 * Instant
 * Scry 2, then draw two cards. You get {E}{E} (two energy counters).
 */
val GlimmerOfGenius = card("Glimmer of Genius") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Scry 2, then draw two cards. You get {E}{E} (two energy counters)."

    spell {
        effect = Effects.Scry(2) then Effects.DrawCards(2) then Effects.GetEnergy(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        artist = "Tommy Arnold"
        flavorText = "Creators live for that moment of inspiration: precious, profound, perfect."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19f1673d-c92c-43be-8648-af7fbc790421.jpg?1783937220"
        ruling(
            "2024-06-07",
            "If an effect says you get one or more {E}, you get that many energy counters. To pay one or more " +
                "{E}, you lose that many energy counters. You can't pay more energy counters than you have. Any " +
                "effects that interact with counters a player gets, has, or loses can interact with energy counters."
        )
        ruling(
            "2024-06-07",
            "Energy counters aren't mana. They don't go away as steps, phases, and turns end, and effects that " +
                "add mana \"of any type\" can't give you energy counters."
        )
    }
}
