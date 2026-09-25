package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity

/**
 * Silent-Chant Zubera
 * {1}{W}
 * Creature — Zubera Spirit
 * 1/2
 * When this creature dies, you gain 2 life for each Zubera that died this turn.
 */
val SilentChantZubera = card("Silent-Chant Zubera") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Zubera Spirit"
    power = 1
    toughness = 2
    oracleText = "When this creature dies, you gain 2 life for each Zubera that died this turn."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.GainLife(DynamicAmounts.creaturesWithSubtypeDiedThisTurn(Subtype("Zubera")) * 2)
        description = "When this creature dies, you gain 2 life for each Zubera that died this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Ben Thompson"
        flavorText = "When the Honden of Cleansing Fire was abandoned, its attendants swarmed Kamigawa to erode mortal will."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/272dfee6-9c90-4409-9289-f451261909e7.jpg?1783944332"
    }
}
