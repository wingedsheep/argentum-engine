package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kazandu Nectarpot — Zendikar Rising #190
 * {1}{G} · Creature — Insect · 1/3
 *
 * Landfall — Whenever a land you control enters, you gain 1 life.
 */
val KazanduNectarpot = card("Kazandu Nectarpot") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    oracleText = "Landfall — Whenever a land you control enters, you gain 1 life."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Simon Dominic"
        flavorText = "\"The scythe-like forelegs are crucial for its survival, because it tastes really good and " +
            "every inhabitant of Murasa knows it.\"\n—Arhana, Kazandu trapfinder"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c9d3f4a-ef83-454e-b8a1-1fc5d5f44be8.jpg"
    }
}
