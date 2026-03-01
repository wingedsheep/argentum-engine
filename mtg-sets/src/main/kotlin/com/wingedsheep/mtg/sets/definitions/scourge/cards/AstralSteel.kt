package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Astral Steel
 * {2}{W}
 * Instant
 * Target creature gets +1/+2 until end of turn.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.
 * You may choose new targets for the copies.)
 */
val AstralSteel = card("Astral Steel") {
    manaCost = "{2}{W}"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+2 until end of turn.\nStorm (When you cast this spell, copy it for each spell cast before it this turn. You may choose new targets for the copies.)"

    spell {
        val t = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(1, 2, t)
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64f836d3-52c8-4628-b18a-8c8fb67969c9.jpg?1562529748"
    }
}
