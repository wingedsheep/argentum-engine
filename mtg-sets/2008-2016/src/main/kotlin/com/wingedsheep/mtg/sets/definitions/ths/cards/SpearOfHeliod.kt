package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spear of Heliod
 * {1}{W}{W}
 * Legendary Enchantment Artifact
 * Creatures you control get +1/+1.
 * {1}{W}{W}, {T}: Destroy target creature that dealt damage to you this turn.
 */
val SpearOfHeliod = card("Spear of Heliod") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Enchantment Artifact"
    oracleText = "Creatures you control get +1/+1.\n" +
        "{1}{W}{W}, {T}: Destroy target creature that dealt damage to you this turn."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl())
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}{W}"), Costs.Tap)
        val t = target(TargetFilter.Creature.dealtDamageToSourceControllerThisTurn())
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Yeong-Hao Han"
        flavorText = "Legend speaks of the Sun Spear, the mighty weapon that can strike any point in Theros, even the depths of the Underworld."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34ef01da-d311-4d4f-9f7b-328e7bb17db4.jpg?1783939806"
    }
}
