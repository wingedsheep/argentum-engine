package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sea Kings' Blessing
 * {U}
 * Instant
 * One or more target creatures become blue until end of turn.
 *
 * "One or more" is an unlimited target count with a floor of one: `unlimited = true, minCount = 1`.
 */
val SeaKingsBlessing = card("Sea Kings' Blessing") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "One or more target creatures become blue until end of turn."

    spell {
        targets(TargetFilter.Creature, unlimited = true, minCount = 1)
        effect = Effects.ForEachTarget(Effects.ChangeColor(EffectTarget.ContextTarget(0), setOf(Color.BLUE)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "75"
        artist = "Randy Asplund-Faith"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11d1f02d-533e-4b77-a72a-ff5f91ae0626.jpg?1783948071"
    }
}
