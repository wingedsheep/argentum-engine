package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Heaven's Gate
 * {W}
 * Instant
 * One or more target creatures become white until end of turn.
 *
 * "One or more" is an unlimited target count with a floor of one: `unlimited = true, minCount = 1`.
 */
val HeavensGate = card("Heaven's Gate") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "One or more target creatures become white until end of turn."

    spell {
        targets(TargetFilter.Creature, unlimited = true, minCount = 1)
        effect = Effects.ForEachTarget(Effects.ChangeColor(EffectTarget.ContextTarget(0), setOf(Color.WHITE)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Douglas Shuler"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/461d7c11-3a7d-42c2-bb6b-0a43779e6842.jpg?1783948084"
    }
}
