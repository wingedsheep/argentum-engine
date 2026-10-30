package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lash of Thorns
 * {B}
 * Instant
 *
 * Target creature gets +2/+1 and gains deathtouch until end of turn.
 */
val LashOfThorns = card("Lash of Thorns") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+1 and gains deathtouch until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 1, t) then
            Effects.GrantKeyword(Keyword.DEATHTOUCH, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Jeff Simpson"
        flavorText = "\"Another angry mob? When will they learn to leave me be?\"\n—Terryn, Edgewall outcast"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea45e972-6e4a-4854-9be3-6ffb24e7d7a5.jpg?1783932638"
    }
}
