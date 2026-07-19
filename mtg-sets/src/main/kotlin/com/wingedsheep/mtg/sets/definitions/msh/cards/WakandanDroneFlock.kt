package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wakandan Drone Flock
 * {3}{W}
 * Artifact Creature — Robot
 * 3/3
 * Flying
 * When this creature enters, scry 2.
 *
 * Implementation note: flying via the engine keyword; the enters trigger reuses the shared
 * `Patterns.Library.scry` recipe rather than a bespoke effect.
 */
val WakandanDroneFlock = card("Wakandan Drone Flock") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Robot"
    oracleText = "Flying\nWhen this creature enters, scry 2. (Look at the top two cards of your library, then put any number of them on the bottom and the rest on top in any order.)"
    power = 3
    toughness = 3
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.scry(2)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Sean Vo"
        flavorText = "Wakandan engineers look to nature for inspiration as they develop new technologies."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4b12dc1f-2218-4c71-aafa-ea6a26eeb0aa.jpg?1783902964"
    }
}
