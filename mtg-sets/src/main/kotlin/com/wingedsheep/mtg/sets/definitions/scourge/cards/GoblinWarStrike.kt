package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

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
        val t = target("target", TargetPlayer())
        effect = DealDamageEffect(
            amount = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withSubtype(Subtype.GOBLIN)
            ).count(),
            target = t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Thomas M. Baxa"
        flavorText = "\"It's not the strategy or the weapons that make goblin attacks so effective. It's the sheer enthusiasm.\"\n—Aven marshal"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dce59945-37a2-4f09-8831-9d44b4a59ea7.jpg?1562535675"
    }
}
