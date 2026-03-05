package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thousand Winds
 * {4}{U}{U}
 * Creature — Elemental
 * 5/6
 * Flying
 * Morph {5}{U}{U}
 * When this creature is turned face up, return all other tapped creatures to their owners' hands.
 */
val ThousandWinds = card("Thousand Winds") {
    manaCost = "{4}{U}{U}"
    typeLine = "Creature — Elemental"
    power = 5
    toughness = 6
    oracleText = "Flying\nMorph {5}{U}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Thousand Winds is turned face up, return all other tapped creatures to their owners' hands."

    keywords(Keyword.FLYING)

    morph = "{5}{U}{U}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Effects.ReturnAllToHand(
            GroupFilter(
                baseFilter = GameObjectFilter.Creature.tapped(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/249b453c-0d5b-4af9-aaec-ebd2f19c5d23.jpg?1562783727"
    }
}
