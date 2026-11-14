package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Consecrated by Blood
 * {2}{B}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +2/+2 and has flying and "Sacrifice two other creatures: Regenerate this
 * creature."
 *
 * The quoted ability is a [GrantActivatedAbility]: its source is the enchanted creature (CR 113.7),
 * so [EffectTarget.Self] regenerates the host and `notSourceItself()` keeps the host out of its own
 * sacrifice cost — "two other creatures" means other than the enchanted creature (Voldaren Pariah's
 * spelling of the same cost).
 */
val ConsecratedByBlood = card("Consecrated by Blood") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +2/+2 and has flying and \"Sacrifice two other creatures: Regenerate this creature.\" " +
        "(The next time the creature would be destroyed this turn, instead tap it, remove it from combat, and heal all damage on it.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.next(),
                cost = Costs.SacrificeMultiple(2, GameObjectFilter.Creature.notSourceItself()),
                effect = Effects.Regenerate(EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d896df9-f897-4786-b1a1-d2375e81c9ef.jpg?1783938345"
        ruling(
            "2015-06-22",
            "If the regeneration ability is activated before combat damage is dealt, the two creatures you " +
                "sacrifice won't deal combat damage. However, you must regenerate a creature before it would be " +
                "destroyed, so if you wait for combat damage to be dealt, the enchanted creature may be destroyed " +
                "by that damage before it has the chance to regenerate."
        )
    }
}
