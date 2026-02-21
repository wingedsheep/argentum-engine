package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Custody Battle
 * {1}{R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature has "At the beginning of your upkeep, target opponent gains
 * control of this creature unless you sacrifice a land."
 */
val CustodyBattle = card("Custody Battle") {
    manaCost = "{1}{R}"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\nEnchanted creature has \"At the beginning of your upkeep, target opponent gains control of this creature unless you sacrifice a land.\""

    auraTarget = Targets.Creature

    triggeredAbility {
        trigger = Triggers.EnchantedCreatureControllerUpkeep
        val t = target("target", TargetOpponent())
        effect = PayOrSufferEffect(
            cost = PayCost.Sacrifice(GameObjectFilter.Land),
            suffer = GiveControlToTargetPlayerEffect(
                permanent = EffectTarget.EnchantedCreature,
                newController = t
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Christopher Rush"
        flavorText = "Everyone wanted it. No one wanted to keep it."
        imageUri = "https://cards.scryfall.io/large/front/b/7/b72257f5-0cf9-45ca-8dc7-a1a93bd7dd1e.jpg?1562938173"
    }
}
