package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mortus Strider
 * {1}{U}{B}
 * Creature — Skeleton
 * 1/1
 * When this creature dies, return it to its owner's hand.
 */
val MortusStrider = card("Mortus Strider") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Skeleton"
    oracleText = "When this creature dies, return it to its owner's hand."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Move(EffectTarget.Self, Zone.HAND)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Tomasz Jedruszek"
        flavorText = "Dimir's best agents hide from death itself."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d644eb6e-cc49-4834-bc2c-53f6a4ceb451.jpg"
    }
}
