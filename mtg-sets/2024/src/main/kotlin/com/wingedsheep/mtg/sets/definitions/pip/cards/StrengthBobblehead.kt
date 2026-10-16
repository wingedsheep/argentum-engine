package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Strength Bobblehead — X (Bobbleheads you control) is counted once, as the counter ability
 * resolves, per the 2024-03-08 ruling.
 */
val StrengthBobblehead = card("Strength Bobblehead") {
    manaCost = "{3}"
    typeLine = "Artifact — Bobblehead"
    oracleText = "{T}: Add one mana of any color.\n" +
        "{3}, {T}: Put X +1/+1 counters on target creature, where X is the number of Bobbleheads you control. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        description = "Put a +1/+1 counter on target creature for each Bobblehead you control."
        val creature = target(TargetFilter.Creature)
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        timing = TimingRule.SorcerySpeed
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Any.withSubtype("Bobblehead")).count(),
            creature,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Nikola Matkovic"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9af73ad-625a-4260-8917-6a837f9fdf88.jpg?1783912375"
        ruling("2024-03-08", "The value of X is determined only once, as Strength Bobblehead’s last ability resolves.")
    }
}
