package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Ganax, Astral Hunter — Commander Legends: Battle for Baldur's Gate #176
 * {4}{R} · Legendary Creature — Dragon · 3/4
 *
 * Flying
 * Whenever Ganax or another Dragon you control enters, create a Treasure token.
 * Choose a Background
 *
 * "Ganax or another Dragon" is one `Triggers.a(...)` over Dragon permanents you control — Ganax is
 * itself a Dragon, so its own arrival matches (Scourge of Valkas's shape). Choose a Background is
 * the deck-construction partner variant.
 */
val GanaxAstralHunter = card("Ganax, Astral Hunter") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dragon"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever Ganax or another Dragon you control enters, create a Treasure token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "Choose a Background (You can have a Background as a second commander.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Dragon").youControl()).enters()
        effect = Effects.CreateTreasure()
        description = "Whenever Ganax or another Dragon you control enters, create a Treasure token."
    }

    keywordAbility(KeywordAbility.chooseABackground())

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "176"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f5bb319-29b2-4a7c-82e9-f8b5e47909a1.jpg?1783922742"
    }
}
