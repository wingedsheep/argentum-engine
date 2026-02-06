package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MayEffect
import com.wingedsheep.sdk.scripting.OnCycle

/**
 * Lightning Rift
 * {1}{R}
 * Enchantment
 * Whenever a player cycles a card, you may pay {1}. If you do,
 * Lightning Rift deals 2 damage to any target.
 *
 * Note: The "pay {1}" cost is approximated as a MayEffect (yes/no choice).
 * Full mana payment on triggered ability resolution would require additional engine support.
 */
val LightningRift = card("Lightning Rift") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment"

    triggeredAbility {
        trigger = OnCycle(controllerOnly = false)
        target = Targets.Any
        effect = MayEffect(
            DealDamageEffect(2, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "217"
        artist = "Dave Dorman"
        flavorText = "\"Never underestimate the power of a good storm.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4a4a901-a284-4322-b88c-bfa1f7e1598d.jpg?1562935348"
    }
}
