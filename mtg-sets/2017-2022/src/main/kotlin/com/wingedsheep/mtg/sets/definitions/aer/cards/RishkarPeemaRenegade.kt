package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rishkar, Peema Renegade
 * {2}{G}
 * Legendary Creature — Elf Druid
 * 2/2
 * When Rishkar enters, put a +1/+1 counter on each of up to two target creatures.
 * Each creature you control with a counter on it has "{T}: Add {G}."
 */
val RishkarPeemaRenegade = card("Rishkar, Peema Renegade") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Druid"
    power = 2
    toughness = 2
    oracleText = "When Rishkar enters, put a +1/+1 counter on each of up to two target creatures.\n" +
        "Each creature you control with a counter on it has \"{T}: Add {G}.\""

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
        description = "When Rishkar enters, put a +1/+1 counter on each of up to two target creatures."
    }

    // "With a counter on it" means any kind of counter, not just +1/+1.
    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddMana(Color.GREEN),
                isManaAbility = true,
                timing = TimingRule.ManaAbility,
                descriptionOverride = "{T}: Add {G}."
            ),
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withAnyCounter())
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Todd Lockwood"
        flavorText = "\"Aether is the soul of Kaladesh, and all souls should be free.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cff0dc6-5455-4dea-940b-dff7fe88dc5d.jpg?1783936739"
    }
}
