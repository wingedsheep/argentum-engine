package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Hide in Plain Sight — Murders at Karlov Manor #166
 * {3}{G} · Sorcery
 *
 * Look at the top five cards of your library, cloak two of them, and put the rest on the bottom of
 * your library in a random order.
 *
 * Straight `lookAtTopAndKeep`: the only cloak-specific part is that the kept cards' move to the
 * battlefield carries [FaceDownMode.CLOAK], which is what makes each of them a face-down 2/2 with
 * ward {2} that can be turned face up for its mana cost if it's a creature card (CR 701.58a/b).
 * Non-creature cards cloaked this way simply have no turn-up procedure, exactly as printed.
 */
val HideInPlainSight = card("Hide in Plain Sight") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library, cloak two of them, and put the rest " +
        "on the bottom of your library in a random order."

    spell {
        effect = Patterns.Library.lookAtTopAndKeep(
            count = 5,
            keepCount = 2,
            keepDestination = CardDestination.ToZone(Zone.BATTLEFIELD),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random,
            keepFaceDown = FaceDownMode.CLOAK
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Vincent Christiaens"
        flavorText = "The trees look beautiful... until they look back."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d87c1ed8-c644-4ad5-9a21-c7bd9a7e8d20.jpg"
    }
}
