package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ember-Fist Zubera
 * {1}{R}
 * Creature — Zubera Spirit
 * 1/2
 * When this creature dies, it deals damage to any target equal to the number of Zubera that died
 * this turn.
 */
val EmberFistZubera = card("Ember-Fist Zubera") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Zubera Spirit"
    power = 1
    toughness = 2
    oracleText = "When this creature dies, it deals damage to any target equal to the number of Zubera that died this turn."

    triggeredAbility {
        trigger = Triggers.self.dies()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.creaturesWithSubtypeDiedThisTurn(Subtype("Zubera")), t)
        description = "When this creature dies, it deals damage to any target equal to the number of Zubera that died this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "166"
        artist = "Ron Spencer"
        flavorText = "When the Honden of Infinite Rage shattered, its attendants swarmed Kamigawa to sow mortal destruction."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/0150e3f0-237e-4669-9401-d2cd08e86387.jpg?1783944300"
    }
}
