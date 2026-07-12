package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Wanderlight Spirit
 * {2}{U}
 * Creature — Spirit
 * 2/3
 * Flying
 * This creature can block only creatures with flying.
 */
val WanderlightSpirit = card("Wanderlight Spirit") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Spirit"
    oracleText = "Flying\nThis creature can block only creatures with flying."
    power = 2
    toughness = 3
    keywords(Keyword.FLYING)
    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "86"
        artist = "Andrew Mar"
        flavorText = "The towers and catwalks of the Voldaren fortress are lit by the lanterns of geists endlessly seeking a way out."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bb3ce5d-330d-427e-a053-8cc4eeb2941b.jpg?1782703131"
    }
}
