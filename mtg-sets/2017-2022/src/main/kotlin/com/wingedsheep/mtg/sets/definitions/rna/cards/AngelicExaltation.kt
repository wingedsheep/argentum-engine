package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Angelic Exaltation
 * {3}{W}
 * Enchantment
 * Whenever a creature you control attacks alone, it gets +X/+X until end of turn,
 * where X is the number of creatures you control.
 */
val AngelicExaltation = card("Angelic Exaltation") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control attacks alone, it gets +X/+X until end of turn, " +
        "where X is the number of creatures you control."

    // X is counted on resolution.
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).attacks(setOf(AttackPredicate.Alone))
        val creatureCount = DynamicAmounts.creaturesYouControl()
        effect = Effects.ModifyStats(creatureCount, creatureCount, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "2"
        artist = "John Severin Brassell"
        flavorText = "\"If we cannot have peace, we will have justice.\"\n—Aurelia"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee7b5ae5-0f6b-4f45-8597-4e6212d2ad0e.jpg?1783933727"
    }
}
