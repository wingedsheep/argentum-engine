package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Plague Stinger
 * {1}{B}
 * Creature — Phyrexian Insect Horror
 * 1/1
 *
 * Flying
 * Infect
 */
val PlagueStinger = card("Plague Stinger") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Insect Horror"
    power = 1
    toughness = 1
    oracleText = "Flying\n" +
        "Infect (This creature deals damage to creatures in the form of -1/-1 counters and to players in the form of poison counters.)"

    keywords(Keyword.FLYING, Keyword.INFECT)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Ryan Pancoast"
        flavorText = "It leaves its victims one sting closer to phyresis."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aae856bc-f65f-42ba-9344-1a30b356c041.jpg?1783941730"
    }
}
