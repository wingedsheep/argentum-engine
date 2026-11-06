package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val PyramidOfThePantheon = card("Pyramid of the Pantheon") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "{2}, {T}: Add one mana of any color. Put a brick counter on this artifact.\n" +
        "{T}: Add three mana of any one color. Activate only if there are three or more brick counters on this artifact."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Effects.AddAnyColorMana(1) then
            Effects.AddCounters(CounterType.BRICK, 1, EffectTarget.Self)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "Add one mana of any color. Put a brick counter on this artifact."
    }

    activatedAbility {
        cost = Costs.Tap
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.SourceCounterCountAtLeast(CounterType.BRICK, 3))
        )
        effect = Effects.AddAnyColorMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "Add three mana of any one color."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Florian de Gesincourt"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cef335b2-8086-4b29-b8e3-47de6faa1334.jpg?1783936448"
    }
}
