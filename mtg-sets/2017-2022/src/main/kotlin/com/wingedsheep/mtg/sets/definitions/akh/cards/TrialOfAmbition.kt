package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Trial of Ambition
 * {1}{B}
 * Enchantment
 * When this enchantment enters, target opponent sacrifices a creature of their choice.
 * When a Cartouche you control enters, return this enchantment to its owner's hand.
 */
val TrialOfAmbition = card("Trial of Ambition") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, target opponent sacrifices a creature of their choice.\n" +
        "When a Cartouche you control enters, return this enchantment to its owner's hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Sacrifice(GameObjectFilter.Creature, target = opponent)
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Permanent.withSubtype("Cartouche").youControl()).enters()
        effect = Effects.ReturnToHand(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Johann Bodin"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db1e79b7-931d-49bf-8c2c-d42c55a93c74.jpg?1783936496"
    }
}
