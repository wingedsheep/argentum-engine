package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ReduceEquipCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Strong Back — Fallout #83
 * {2}{G} · Enchantment — Aura
 *
 * Enchant creature
 * Equip abilities you activate that target enchanted creature cost {3} less to activate.
 * Aura spells you cast that target enchanted creature cost {3} less to cast.
 * Enchanted creature gets +2/+2 for each Aura and Equipment attached to it.
 *
 * Both discounts are scoped by `GameObjectFilter.Creature.attachedToBySource()`, evaluated relative
 * to Strong Back: [ReduceEquipCost.targetFilter] for equip, and a target-conditional
 * [CostReductionSource.FixedIfAnyTargetMatches] on a [SpellCostTarget.YouCast] Aura modifier for
 * spells. Each reduces generic mana only (ruling 2024-03-08).
 */
val StrongBack = card("Strong Back") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Equip abilities you activate that target enchanted creature cost {3} less to activate.\n" +
        "Aura spells you cast that target enchanted creature cost {3} less to cast.\n" +
        "Enchanted creature gets +2/+2 for each Aura and Equipment attached to it."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ReduceEquipCost(amount = 3, targetFilter = GameObjectFilter.Creature.attachedToBySource())
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Enchantment.withSubtype(Subtype.AURA)),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.FixedIfAnyTargetMatches(3, GameObjectFilter.Creature.attachedToBySource())
            )
        )
    }

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.attachmentsOnEnchantedCreature() * 2,
            toughnessBonus = DynamicAmounts.attachmentsOnEnchantedCreature() * 2
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Elizabeth Peiró"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f65afa5-e4f7-49c2-97f9-47ad5dbcacfa.jpg?1783912396"
        ruling("2024-03-08", "Strong Back’s abilities reduce only the amount of generic mana in equip abilities that target the enchanted creature and in the total cost of Aura spells that target the enchanted creature. For example, it will reduce the total cost of Animal Friend from {1}{G} to {G}.")
        ruling("2024-03-08", "Strong Back’s third ability won’t reduce its own cost.")
        ruling("2024-03-08", "Some Equipment creature cards in other sets have reconfigure, a different ability that attaches them to a creature. Reconfigure is not an equip ability, and reconfigure costs are not reduced by Strong Back’s second ability. Equip variants or equip abilities with restrictions, like the “equip Citizen” ability of Ceremonial Groundbreaker from the Streets of New Capenna® set, are still equip abilities, and their costs will be reduced by Strong Back’s second ability.")
    }
}
