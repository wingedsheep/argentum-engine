package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thorned Moloch
 * {2}{R}
 * Creature — Lizard
 * 2/2
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * This creature has first strike as long as it's attacking.
 */
val ThornedMoloch = card("Thorned Moloch") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Lizard"
    power = 2
    toughness = 2
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "This creature has first strike as long as it's attacking."

    prowess()

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.source())
        condition = Conditions.SourceIsAttacking
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "108"
        artist = "James Paick"
        flavorText = "Those who fled the destruction of the city stumbled into the territory of their prickly neighbors."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/01ab2615-5a02-4b59-b8bd-577b91a1a0e3.jpg?1783936024"
    }
}
