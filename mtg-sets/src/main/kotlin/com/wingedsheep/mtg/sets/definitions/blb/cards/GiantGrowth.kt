package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Giant Growth
 * {G}
 * Instant
 *
 * Target creature gets +3/+3 until end of turn.
 */
val GiantGrowth = card("Giant Growth") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +3/+3 until end of turn."

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.ModifyStats(3, 3, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "393"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e70722d6-b4d5-45c2-9488-9a5eb0bdb9bd.jpg?1721428103"
        inBooster = false
    }
}
