package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * A.I.M. Synthoids
 * {2}
 * Artifact Creature — Robot Villain
 * 1/3
 *
 * When this creature enters, surveil 2.
 *
 * Surveil is the shared [Patterns.Library.surveil] pipeline (look at the top N, distribute between
 * graveyard and library top).
 */
val AimSynthoids = card("A.I.M. Synthoids") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Robot Villain"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, surveil 2. (Look at the top two cards of your library, " +
        "then put any number of them into your graveyard and the rest on top of your library in any order.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.surveil(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "242"
        artist = "Alexander Skripnikov"
        flavorText = "\"M.O.D.O.K.—commands—that—you—be—neutralized! Nothing—must—interfere—with—our—programmed—mission!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f29dee50-ea7e-4a54-bc91-99928a8405e3.jpg?1783902892"
    }
}
