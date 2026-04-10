package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostByFilter

/**
 * Stormcatch Mentor
 * {U}{R}
 * Creature — Otter Wizard
 * 1/1
 *
 * Haste
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * Instant and sorcery spells you cast cost {1} less to cast.
 */
val StormcatchMentor = card("Stormcatch Mentor") {
    manaCost = "{U}{R}"
    typeLine = "Creature — Otter Wizard"
    power = 1
    toughness = 1
    oracleText = "Haste\nProwess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\nInstant and sorcery spells you cast cost {1} less to cast."

    keywords(Keyword.HASTE)
    prowess()

    staticAbility {
        ability = ReduceSpellCostByFilter(
            filter = GameObjectFilter.InstantOrSorcery,
            amount = 1
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "234"
        artist = "Manuel Castañón"
        flavorText = "\"Only novice mages burn their whiskers. So pay attention!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99754055-6d67-4fde-aff3-41f6af6ea764.jpg?1721427197"
    }
}
