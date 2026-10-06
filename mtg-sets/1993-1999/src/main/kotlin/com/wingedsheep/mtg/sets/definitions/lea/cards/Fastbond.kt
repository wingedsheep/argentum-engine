package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.GrantAdditionalLandDrop
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fastbond
 * {G}
 * Enchantment
 * You may play any number of lands on each of your turns.
 * Whenever you play a land, if it wasn't the first land you played this turn, this enchantment
 * deals 1 damage to you.
 *
 * "Any number" is `GrantAdditionalLandDrop(count = null)` — unbounded, not a large constant.
 * "Wasn't the first land" is an intervening if on the lands-played tracker, which already counts
 * the land that triggered it, so lands played before Fastbond entered count too.
 */
val Fastbond = card("Fastbond") {
    manaCost = "{G}"
    typeLine = "Enchantment"
    oracleText = "You may play any number of lands on each of your turns.\n" +
        "Whenever you play a land, if it wasn't the first land you played this turn, " +
        "this enchantment deals 1 damage to you."
    staticAbility {
        ability = GrantAdditionalLandDrop(count = null)
    }
    triggeredAbility {
        trigger = Triggers.you.playsLand()
        interveningIf = Conditions.CompareAmounts(DynamicAmounts.landsPlayedThisTurn(), ComparisonOperator.GTE, 2)
        effect = Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "192"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a575a9af-e1de-4a1d-91d8-440585377e4f.jpg?1783948678"
    }
}
