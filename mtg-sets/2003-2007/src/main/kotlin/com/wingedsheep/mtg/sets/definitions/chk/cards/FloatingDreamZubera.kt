package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Floating-Dream Zubera
 * {1}{U}
 * Creature — Zubera Spirit
 * 1/2
 * When this creature dies, draw a card for each Zubera that died this turn.
 */
val FloatingDreamZubera = card("Floating-Dream Zubera") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zubera Spirit"
    power = 1
    toughness = 2
    oracleText = "When this creature dies, draw a card for each Zubera that died this turn."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(DynamicAmounts.creaturesWithSubtypeDiedThisTurn(Subtype("Zubera")))
        description = "When this creature dies, draw a card for each Zubera that died this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "61"
        artist = "Shishizaru"
        flavorText = "When the Honden of Seeing Winds was forgotten, its attendants swarmed Kamigawa to uncover mortal secrets."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/63ea55a8-6c21-4cc1-a2c4-86cd60cddced.jpg?1783944328"
    }
}
