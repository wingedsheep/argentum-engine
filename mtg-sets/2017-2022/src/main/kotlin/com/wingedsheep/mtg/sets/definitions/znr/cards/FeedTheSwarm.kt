package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Feed the Swarm — Zendikar Rising #102 (canonical printing; reprinted in Foundations)
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
        val t = target(TargetFilter.CreatureOrEnchantment.opponentControls())
        effect = Effects.Destroy(t) then
            Effects.LoseLife(
                DynamicAmounts.manaValueOf(t),
                EffectTarget.Controller
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"On vile wings and bloody wind, the swarm will rise.\"\n—Skyclave inscription"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6b2eba7-862a-4efd-9f65-065fb2070855.jpg"
    }
}
