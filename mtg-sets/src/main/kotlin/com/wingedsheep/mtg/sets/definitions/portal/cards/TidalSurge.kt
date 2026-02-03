package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TapTargetCreaturesEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

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
        // Use targeting DSL to declare valid targets - up to 3 creatures without flying
        target = TargetCreature(
            count = 3,
            optional = true,
            unifiedFilter = TargetFilter.Creature.withoutKeyword(Keyword.FLYING)
        )
        // Effect taps all selected targets
        effect = TapTargetCreaturesEffect(maxTargets = 3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "Drew Tucker"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a027c31d-c662-4ce1-a0d1-a32e62f6a724.jpg"
    }
}
