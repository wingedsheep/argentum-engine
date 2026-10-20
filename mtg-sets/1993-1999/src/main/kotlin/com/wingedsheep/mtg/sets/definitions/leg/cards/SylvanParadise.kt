package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sylvan Paradise
 * {G}
 * Instant
 * One or more target creatures become green until end of turn.
 *
 * "One or more" is an unlimited target count with a floor of one: `unlimited = true, minCount = 1`.
 */
val SylvanParadise = card("Sylvan Paradise") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "One or more target creatures become green until end of turn."

    spell {
        targets(TargetFilter.Creature, unlimited = true, minCount = 1)
        effect = Effects.ForEachTarget(Effects.ChangeColor(EffectTarget.ContextTarget(0), setOf(Color.GREEN)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "208"
        artist = "Randy Asplund-Faith"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f323c3bb-cece-4035-b1a7-c4817cf7a08c.jpg?1783948043"
    }
}
