package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eyes of the Beholder
 * {4}{B}{B}
 * Instant
 * Target creature gets -11/-11 until end of turn.
 */
val EyesOfTheBeholder = card("Eyes of the Beholder") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -11/-11 until end of turn."
    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-11, -11, t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Kari Christensen"
        flavorText = "In the end, there was nothing beautiful to be seen."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f9e5402-690f-497d-ada3-aa41fa900fdd.jpg?1783926497"
    }
}
