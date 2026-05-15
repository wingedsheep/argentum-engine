package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blasphemous Act {8}{R}
 * Sorcery
 *
 * This spell costs {1} less to cast for each creature on the battlefield.
 * Blasphemous Act deals 13 damage to each creature.
 */
val BlasphemousAct = card("Blasphemous Act") {
    manaCost = "{8}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "This spell costs {1} less to cast for each creature on the battlefield.\n" +
        "Blasphemous Act deals 13 damage to each creature."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsOnBattlefieldMatching(Filters.Creature)
            ),
        )
    }

    spell {
        effect = ForEachInGroupEffect(
            filter = GroupFilter.AllCreatures,
            effect = DealDamageEffect(13, EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Justine Cruz"
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fb01696a-855c-45af-804c-d41acb6496e1.jpg?1721428730"
        flavorText = "In the early days of Valley, desperate animalfolk harnessed the power of Calamity Beasts in ways that destroyed more than defended."
        ruling("2020-11-10", "Although players may respond to Blasphemous Act once it's been cast, once it's announced, they can't respond before the cost is calculated and paid.")
        ruling("2020-11-10", "Blasphemous Act's ability can't reduce the total cost to cast the spell below {R}.")
        ruling("2020-11-10", "The total cost to cast Blasphemous Act is locked in before you pay that cost.")
    }
}
