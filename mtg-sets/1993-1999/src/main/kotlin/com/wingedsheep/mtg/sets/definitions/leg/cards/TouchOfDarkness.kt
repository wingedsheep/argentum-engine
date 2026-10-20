package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Touch of Darkness
 * {B}
 * Instant
 * One or more target creatures become black until end of turn.
 *
 * "One or more" is an unlimited target count with a floor of one: `unlimited = true, minCount = 1`.
 */
val TouchOfDarkness = card("Touch of Darkness") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "One or more target creatures become black until end of turn."

    spell {
        targets(TargetFilter.Creature, unlimited = true, minCount = 1)
        effect = Effects.ForEachTarget(Effects.ChangeColor(EffectTarget.ContextTarget(0), setOf(Color.BLACK)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "122"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/eda7177f-1354-4008-aaaa-2c8b823ed5e9.jpg?1783948061"
    }
}
