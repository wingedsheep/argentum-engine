package com.wingedsheep.mtg.sets.definitions.gpt.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wildsize
 * {2}{G}
 * Instant
 *
 * Target creature gets +2/+2 and gains trample until end of turn.
 * Draw a card.
 */
val Wildsize = card("Wildsize") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 and gains trample until end of turn.\nDraw a card."

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.Composite(
            Effects.ModifyStats(2, 2, t),
            Effects.GrantKeyword(Keyword.TRAMPLE, t),
            Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Jim Murray"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eef061ed-736c-41ba-b692-d1fbfaca242e.jpg?1593272542"
    }
}
