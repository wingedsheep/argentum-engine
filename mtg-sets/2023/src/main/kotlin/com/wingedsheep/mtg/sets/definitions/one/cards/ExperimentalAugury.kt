package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Experimental Augury — Phyrexia: All Will Be One #49
 * {1}{U}
 * Instant
 * Look at the top three cards of your library. Put one of them into your hand and the rest on
 * the bottom of your library in any order. Proliferate.
 */
val ExperimentalAugury = card("Experimental Augury") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Look at the top three cards of your library. Put one of them into your hand and the rest on the bottom of your library in any order. Proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    spell {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 3,
            keepCount = 1,
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.ControllerChooses
        ) then Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e508ae5d-ffb5-4480-be90-a4394954b559.jpg?1783918065"
    }
}
