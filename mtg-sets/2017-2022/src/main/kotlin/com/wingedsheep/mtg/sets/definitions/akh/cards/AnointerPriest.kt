package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.embalm
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Anointer Priest — Amonkhet #3
 * {1}{W} · Creature — Human Cleric · Common
 * 1/3
 *
 * Whenever a creature token you control enters, you gain 1 life.
 * Embalm {3}{W}
 *
 * The embalmed token is a copy with the same ability, so it sees itself enter and gains 1 life.
 */
val AnointerPriest = card("Anointer Priest") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "Whenever a creature token you control enters, you gain 1 life.\n" +
        "Embalm {3}{W} ({3}{W}, Exile this card from your graveyard: Create a token that's a copy of it, " +
        "except it's a white Zombie Human Cleric with no mana cost. Embalm only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.token().youControl()).enters()
        effect = Effects.GainLife(1)
    }

    embalm("{3}{W}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f922231-bf41-47d3-8fa3-13b9baf4a0f3.jpg?1783936543"
    }
}
