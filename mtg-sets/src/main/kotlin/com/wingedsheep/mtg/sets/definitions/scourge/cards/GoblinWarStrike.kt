package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetPlayer

/**
 * Goblin War Strike
 * {R}
 * Sorcery
 * Goblin War Strike deals damage to target player equal to the number of
 * Goblins you control.
 */
val GoblinWarStrike = card("Goblin War Strike") {
    manaCost = "{R}"
    typeLine = "Sorcery"
    oracleText = "Goblin War Strike deals damage to target player equal to the number of Goblins you control."

    spell {
        target = TargetPlayer()
        effect = DealDamageEffect(
            amount = DynamicAmounts.battlefield(
                com.wingedsheep.sdk.scripting.Player.You,
                com.wingedsheep.sdk.scripting.GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)
            ).count(),
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Thomas M. Baxa"
        flavorText = "\"It's not the strategy or the weapons that make goblin attacks so effective. It's the sheer enthusiasm.\"\n—Aven marshal"
        imageUri = "https://cards.scryfall.io/large/front/e/1/e1a4c25c-7cc5-4101-b95d-8f10afb58d44.jpg?1562536355"
    }
}
