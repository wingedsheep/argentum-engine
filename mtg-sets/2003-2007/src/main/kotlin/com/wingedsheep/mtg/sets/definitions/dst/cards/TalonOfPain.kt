package com.wingedsheep.mtg.sets.definitions.dst.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val TalonOfPain = card("Talon of Pain") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "Whenever a source you control other than this artifact deals damage to an opponent, " +
        "put a charge counter on this artifact.\n" +
        "{X}, {T}, Remove X charge counters from this artifact: It deals X damage to any target."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Any.youControl()).dealsDamage(Recipient.Opponent)
        // The source exclusion is checked only when damage triggers the ability.
        triggerRestriction = Conditions.EntityMatches(EffectTarget.TriggeringEntity, GameObjectFilter.Any.notSourceItself())
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{X}"),
            Costs.Tap,
            Costs.RemoveXCounters(counterType = CounterType.CHARGE, self = true),
        )
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb4ab5e0-5f6b-422d-8ba7-9e0ce1450af8.jpg?1783944416"
    }
}
