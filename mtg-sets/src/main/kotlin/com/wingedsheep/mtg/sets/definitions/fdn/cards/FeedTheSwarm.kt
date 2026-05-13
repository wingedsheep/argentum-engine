package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Feed the Swarm
 * {1}{B}
 * Sorcery
 *
 * Destroy target creature or enchantment an opponent controls. You lose life equal to
 * that permanent's mana value.
 *
 * Ruling: life loss is based on the permanent's mana value as it last existed on the
 * battlefield. If the target is indestructible (not destroyed), you still lose life.
 */
val FeedTheSwarm = card("Feed the Swarm") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature or enchantment an opponent controls. You lose life equal to that permanent's mana value."

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.CreatureOrEnchantment.opponentControls()))
        effect = Effects.Composite(
            Effects.Destroy(t),
            Effects.LoseLife(
                DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.ManaValue),
                EffectTarget.Controller
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "712"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"On vile wings and bloody wind, the swarm will rise.\"\n—Skyclave inscription"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e66d4541-160c-4137-98e7-0eaa692c7d7a.jpg?1742922285"
    }
}
