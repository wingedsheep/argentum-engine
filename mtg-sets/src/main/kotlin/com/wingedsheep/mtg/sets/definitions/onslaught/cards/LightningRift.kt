package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.dsl.Triggers

/**
 * Lightning Rift
 * {1}{R}
 * Enchantment
 * Whenever a player cycles a card, you may pay {1}. If you do,
 * Lightning Rift deals 2 damage to any target.
 */
val LightningRift = card("Lightning Rift") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment"
    oracleText = "Whenever a player cycles a card, you may pay {1}. If you do, Lightning Rift deals 2 damage to any target."

    triggeredAbility {
        trigger = Triggers.AnyPlayerCycles
        val t = target("target", Targets.Any)
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = DealDamageEffect(2, t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Eric Peterson"
        flavorText = "\"Never underestimate the power of a good storm.\""
        imageUri = "https://cards.scryfall.io/large/front/d/7/d775d729-0ad9-4b14-9d44-6282f6936e07.jpg?1562946381"
    }
}
