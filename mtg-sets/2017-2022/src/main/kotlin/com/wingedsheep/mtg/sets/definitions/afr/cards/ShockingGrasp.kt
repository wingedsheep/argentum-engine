package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Shocking Grasp
 * {1}{U}
 * Instant
 * Target creature gets -2/-0 until end of turn.
 * Draw a card.
 */
val ShockingGrasp = card("Shocking Grasp") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature gets -2/-0 until end of turn.\nDraw a card."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-2, 0, t) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Jason Felix"
        flavorText = "\"Metal armor? A poor choice.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab95fe13-c09b-4eed-aeae-fb1c5b6eb476.jpg?1783926509"
    }
}
