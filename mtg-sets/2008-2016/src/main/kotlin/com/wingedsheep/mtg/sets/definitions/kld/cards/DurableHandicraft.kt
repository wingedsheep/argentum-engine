package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Durable Handicraft
 * {1}{G}
 * Enchantment
 * Whenever a creature you control enters, you may pay {1}. If you do, put a +1/+1 counter on that creature.
 * {5}{G}, Sacrifice this enchantment: Put a +1/+1 counter on each creature you control.
 */
val DurableHandicraft = card("Durable Handicraft") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control enters, you may pay {1}. If you do, put a +1/+1 counter on that creature.\n" +
        "{5}{G}, Sacrifice this enchantment: Put a +1/+1 counter on each creature you control."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{1}"),
            then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
        )
        description = "Whenever a creature you control enters, you may pay {1}. If you do, put a +1/+1 counter on that creature."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}{G}"), Costs.SacrificeSelf)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28d5e2ff-dc8a-4ed5-989a-4b4b79591b3f.jpg?1783937179"
    }
}
