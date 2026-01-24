package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreatureTargetFilter
import com.wingedsheep.sdk.scripting.TapTargetCreaturesEffect

/**
 * Tidal Surge
 * {1}{U}
 * Sorcery
 * Tap up to three target creatures without flying.
 */
val TidalSurge = card("Tidal Surge") {
    manaCost = "{1}{U}"
    typeLine = "Sorcery"

    spell {
        effect = TapTargetCreaturesEffect(
            maxTargets = 3,
            filter = CreatureTargetFilter.WithoutFlying
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Drew Tucker"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91d4d5e5-67de-4fa1-bb7e-5f48e2b10587.jpg"
    }
}
