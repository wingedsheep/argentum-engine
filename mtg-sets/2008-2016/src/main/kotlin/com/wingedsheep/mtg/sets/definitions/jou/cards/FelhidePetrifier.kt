package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Felhide Petrifier
 * {2}{B}
 * Creature — Minotaur Warrior
 * 2/3
 *
 * Deathtouch
 * Other Minotaur creatures you control have deathtouch.
 *
 * The Petrifier's own deathtouch is printed; the lord grant excludes it (`excludeSelf` is "Other").
 */
val FelhidePetrifier = card("Felhide Petrifier") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Minotaur Warrior"
    power = 2
    toughness = 3
    oracleText = "Deathtouch\nOther Minotaur creatures you control have deathtouch."

    keywords(Keyword.DEATHTOUCH)

    staticAbility {
        ability = GrantKeyword(
            Keyword.DEATHTOUCH,
            GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.MINOTAUR).youControl(), excludeSelf = true),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "James Ryman"
        flavorText = "It's common practice among minotaurs to collect the heads of their victims as trophies. " +
            "Sometimes the trophies prove more than ornamental."
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d6cce01-a0cb-4059-a0ad-fbf7d9b998b8.jpg?1783939435"
    }
}
