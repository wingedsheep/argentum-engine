package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sungold Barrage
 * {2}{W}
 * Instant
 * Destroy target creature with toughness 4 or greater.
 */
val SungoldBarrage = card("Sungold Barrage") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target creature with toughness 4 or greater."

    spell {
        val t = target(TargetFilter.Creature.toughnessAtLeast(4))
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Leanna Crossan"
        flavorText = "Heida allowed herself a moment of satisfaction as she watched the beast fall, then turned to seek another foe. It would be a long, long night."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee507688-9890-47c4-bb04-43c51eb48e22.jpg?1783925650"
    }
}
