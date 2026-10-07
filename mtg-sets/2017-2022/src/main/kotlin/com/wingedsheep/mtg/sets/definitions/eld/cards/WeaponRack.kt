package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val WeaponRack = card("Weapon Rack") {
    manaCost = "{4}"
    typeLine = "Artifact"
    oracleText = "This artifact enters with three +1/+1 counters on it.\n" +
        "{T}: Move a +1/+1 counter from this artifact onto target creature. Activate only as a sorcery."

    replacementEffect(EntersWithCounters(count = 3, selfOnly = true))

    activatedAbility {
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.Creature)
        effect = Effects.MoveCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.fixed(1),
            source = EffectTarget.Self,
            destination = creature
        )
        description = "Move a +1/+1 counter onto target creature. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "236"
        artist = "Joe Slucher"
        flavorText = "No weapon stays on the rack for long in the Burning Yard."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89ca22d2-3ba5-4173-9c8c-6587a901ff4a.jpg?1783932580"
        ruling("2019-10-04", "Once Weapon Rack runs out of +1/+1 counters, it remains on the battlefield. You can activate its last ability, but it won't do anything.")
        ruling("2019-10-04", "If Weapon Rack has left the battlefield or has no +1/+1 counters on it by the time its activated ability resolves, you won't put a +1/+1 counter on the target creature. If the creature becomes an illegal target or can't have a +1/+1 counter put onto it for some other reason, you won't remove a +1/+1 counter from Weapon Rack.")
    }
}
