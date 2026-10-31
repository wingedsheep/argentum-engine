package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Gauntlets of Light — Core Set 2020 #17
 * {2}{W} · Enchantment — Aura · Uncommon
 *
 * Enchant creature
 * Enchanted creature gets +0/+2 and assigns combat damage equal to its toughness rather than its power.
 * Enchanted creature has "{2}{W}: Untap this creature."
 *
 * The damage substitution is the unconditional form of [AssignDamageEqualToToughness]
 * (`onlyWhenToughnessGreaterThanPower = false`) scoped to the attached creature — read by the combat
 * damage step off the Aura itself, so it changes only the damage assigned, never the creature's power
 * (fight and other power checks use real power, per the 2019-07-12 ruling).
 */
val GauntletsOfLight = card("Gauntlets of Light") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +0/+2 and assigns combat damage equal to its toughness rather than its power.\n" +
        "Enchanted creature has \"{2}{W}: Untap this creature.\""

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(0, +2, Filters.EnchantedCreature)
    }

    staticAbility {
        ability = AssignDamageEqualToToughness(
            filter = GroupFilter.attachedCreature(),
            onlyWhenToughnessGreaterThanPower = false,
        )
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.Mana("{2}{W}"),
                effect = Effects.Untap(EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da0d5436-b881-45ac-b8ec-248d88714021.jpg?1783933028"
        ruling(
            "2019-07-12",
            "Gauntlets of Light's effect doesn't change the enchanted creature's power; it changes only " +
                "the amount of combat damage that creature assigns. All other rules and effects that check " +
                "power or toughness use the actual values. For example, if a creature fights while enchanted " +
                "by Gauntlets of Light, that creature will deal damage equal to its power, not its toughness."
        )
    }
}
