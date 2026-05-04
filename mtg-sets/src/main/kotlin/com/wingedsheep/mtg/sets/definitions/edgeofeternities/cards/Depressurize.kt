package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Depressurize
 * {1}{B}
 * Instant
 * Target creature gets -3/-0 until end of turn. Then if that creature's power is 0 or less, destroy it.
 */
val Depressurize = card("Depressurize") {
    manaCost = "{1}{B}"
    typeLine = "Instant"
    oracleText = "Target creature gets -3/-0 until end of turn. Then if that creature's power is 0 or less, destroy it."

    // Main spell effect
    spell {
        val target = target("target creature", Targets.Creature)
        
        // Apply -3/-0 debuff
        effect = Effects.ModifyStats(-3, 0, target)
            .then(
                // Then destroy if power is 0 or less
                ConditionalEffect(
                    condition = Conditions.TargetPowerAtMost(DynamicAmount.Fixed(0)),
                    effect = Effects.Destroy(target)
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Danny Schwartz"
        flavorText = "The Sunstar faith teaches of Perfect Void—of emptying oneself so that the light may fill the hollow. But in the void of death, its teachings bring little comfort."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25520d5a-1a83-42cc-8ace-8b1156019d64.jpg?1752946939"
    }
}
