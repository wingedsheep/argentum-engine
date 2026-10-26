package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Abomination of Llanowar
 * {1}{B}{G}
 * Legendary Creature — Elf Horror
 * * / *
 * Vigilance; menace
 * Abomination of Llanowar's power and toughness are each equal to the number of Elves you control
 * plus the number of Elf cards in your graveyard.
 *
 * "Elves you control" counts every Elf permanent (Gloom Ripper's reading); the characteristic-defining
 * P/T works in every zone, so the Abomination counts itself on the battlefield and in the graveyard.
 */
val AbominationOfLlanowar = card("Abomination of Llanowar") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Elf Horror"
    oracleText = "Vigilance; menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Abomination of Llanowar's power and toughness are each equal to the number of Elves you control " +
        "plus the number of Elf cards in your graveyard."

    keywords(Keyword.VIGILANCE, Keyword.MENACE)

    dynamicStats(
        DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype(Subtype.ELF)).count() +
            DynamicAmounts.zone(Player.You, Zone.GRAVEYARD, GameObjectFilter.Any.withSubtype(Subtype.ELF)).count()
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "265"
        artist = "Vincent Proce"
        flavorText = "\"Run!\" screamed its living mouths. \"Come!\" cried its dead ones."
        imageUri = "https://cards.scryfall.io/normal/front/5/6/567dbc64-4d59-4bab-a551-08fc66c085fa.jpg?1783928779"
        ruling("2020-11-10", "The ability that defines Abomination of Llanowar's power and toughness applies in all zones, not just the battlefield.")
        ruling("2020-11-10", "As long as Abomination of Llanowar is on the battlefield (and still an Elf) or in your graveyard, its ability will count itself.")
    }
}
