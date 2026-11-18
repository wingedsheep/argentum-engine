package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells

/**
 * Chief Engineer
 * {1}{U}
 * Creature — Vedalken Artificer
 * 1/3
 *
 * Artifact spells you cast have convoke. (Your creatures can help cast those spells. Each creature
 * you tap while casting an artifact spell pays for {1} or one mana of that creature's color.)
 */
val ChiefEngineer = card("Chief Engineer") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Vedalken Artificer"
    power = 1
    toughness = 3
    oracleText = "Artifact spells you cast have convoke. (Your creatures can help cast those spells. " +
        "Each creature you tap while casting an artifact spell pays for {1} or one mana of that " +
        "creature's color.)"

    staticAbility {
        ability = GrantKeywordToOwnSpells(
            keyword = Keyword.CONVOKE,
            spellFilter = GameObjectFilter.Artifact,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Steven Belledin"
        flavorText = "An eye for detail, a mind for numbers, a soul of clockwork."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/483189ed-d3d9-4612-ab95-f4ae4e091238.jpg?1783939194"
        ruling("2014-07-18", "Multiple instances of convoke on a single spell are redundant.")
    }
}
