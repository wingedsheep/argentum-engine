package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * High Alert
 * {1}{W}{U}
 * Enchantment
 * Each creature you control assigns combat damage equal to its toughness rather than its power.
 * Creatures you control can attack as though they didn't have defender.
 * {2}{W}{U}: Untap target creature.
 *
 * The toughness-damage static is unconditional (unlike Ghalta's), scoped to creatures you control.
 */
val HighAlert = card("High Alert") {
    manaCost = "{1}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Enchantment"
    oracleText = "Each creature you control assigns combat damage equal to its toughness rather than its power.\n" +
        "Creatures you control can attack as though they didn't have defender.\n" +
        "{2}{W}{U}: Untap target creature."

    staticAbility {
        ability = AssignDamageEqualToToughness(
            filter = GroupFilter.AllCreaturesYouControl,
            onlyWhenToughnessGreaterThanPower = false,
        )
    }

    staticAbility {
        ability = CanAttackDespiteDefender(filter = GroupFilter.AllCreaturesYouControl)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}{U}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce124f6e-ef0c-4d01-a876-e34d3e445108.jpg?1783933647"
        ruling(
            "2019-01-25",
            "High Alert's first ability doesn't actually change any creature's power. It changes only the amount " +
                "of combat damage it assigns. All other rules and effects that check power or toughness use the real values. " +
                "For example, Titanic Brawl won't cause a creature to fight with its toughness under High Alert.",
        )
    }
}
