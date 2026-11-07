package com.wingedsheep.mtg.sets.definitions.csp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Krovikan Mist
 * {1}{U}
 * Creature — Illusion
 * * / *
 *
 * Flying
 * Krovikan Mist's power and toughness are each equal to the number of Illusions on the battlefield.
 *
 * "Illusions on the battlefield" counts every Illusion permanent, under any player's control —
 * including the Mist itself, so it is at least 1/1.
 */
val KrovikanMist = card("Krovikan Mist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion"
    oracleText = "Flying\n" +
        "Krovikan Mist's power and toughness are each equal to the number of Illusions on the battlefield."

    keywords(Keyword.FLYING)

    dynamicStats(DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Permanent.withSubtype("Illusion")).count())

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "38"
        artist = "Jeremy Jarvis"
        flavorText = "Plague-induced dreams became real as they swirled above Krov, mingling with the magical residue of the World Spell."
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43e09469-205d-4a60-821c-ceb7ae2e01be.jpg?1783943358"
    }
}
