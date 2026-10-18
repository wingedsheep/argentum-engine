package com.wingedsheep.mtg.sets.definitions.neo.cards

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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Grafted Growth — Kamigawa: Neon Dynasty #188
 * {2}{G} · Enchantment — Aura
 *
 * Enchant land
 * When this Aura enters, put a +1/+1 counter on target creature or Vehicle you control.
 * Enchanted land has "{T}: Add two mana of any one color."
 *
 * New Horizons with the NEO "creature or Vehicle" target widening
 * ([GameObjectFilter.CreatureOrVehicle], so an uncrewed Vehicle is a legal target). The granted
 * ability is a separate mana ability on the land, hence [GrantActivatedAbility] onto the attached
 * permanent rather than the Fertile Ground `AdditionalManaOnTap` shape.
 */
val GraftedGrowth = card("Grafted Growth") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When this Aura enters, put a +1/+1 counter on target creature or Vehicle you control.\n" +
        "Enchanted land has \"{T}: Add two mana of any one color.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val target = target(TargetFilter(GameObjectFilter.CreatureOrVehicle.youControl()))
        effect = Effects.AddCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 1,
            target = target
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Tap,
                effect = Effects.AddAnyColorMana(2),
                isManaAbility = true,
                timing = TimingRule.ManaAbility
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "188"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7a15010-1b70-4b4f-8b5d-cb2d764a1799.jpg?1783923849"
    }
}
