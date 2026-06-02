package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Costs

/**
 * Vile Consumption
 * {1}{U}{B}
 * Enchantment
 * All creatures have "At the beginning of your upkeep, sacrifice this creature unless you pay 1 life."
 *
 * Implemented as a [GrantTriggeredAbility] static ability over [GroupFilter.AllCreatures]: each
 * creature gains its own beginning-of-upkeep trigger that fires on its controller's upkeep
 * ([Triggers.YourUpkeep]). [PayOrSufferEffect] lets that controller (EffectTarget.Controller) pay 1
 * life; otherwise the granted creature itself (EffectTarget.Self) is sacrificed.
 */
val VileConsumption = card("Vile Consumption") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Enchantment"
    oracleText = "All creatures have \"At the beginning of your upkeep, sacrifice this creature unless you pay 1 life.\""

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.YourUpkeep.event,
                binding = Triggers.YourUpkeep.binding,
                effect = PayOrSufferEffect(
                    cost = Costs.pay.PayLife(1),
                    suffer = Effects.SacrificeTarget(EffectTarget.Self)
                )
            ),
            filter = GroupFilter.AllCreatures
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "285"
        artist = "Heather Hudson"
        flavorText = "The plague moved faster than an army and was far more deadly."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f7e5716-77f3-45d2-a40a-f5bf500f6ad7.jpg?1562920730"
    }
}
