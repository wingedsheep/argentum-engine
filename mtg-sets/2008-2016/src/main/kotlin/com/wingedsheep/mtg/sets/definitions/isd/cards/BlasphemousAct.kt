package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

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
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreatures,
            effect = Effects.DealDamage(13, EffectTarget.IterationEntity),
        )
    }

    metadata {
        flavorText = "\"Holy places are no longer sanctuary from death, and death is no longer sanctuary from anything.\"\n—Thalia, Knight-Cathar"
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/509ce648-fb76-486d-8b39-183e368b7cb7.jpg"
        ruling("2020-11-10", "Although players may respond to Blasphemous Act once it's been cast, once it's announced, they can't respond before the cost is calculated and paid.")
        ruling("2020-11-10", "Blasphemous Act's ability can't reduce the total cost to cast the spell below {R}.")
        ruling("2020-11-10", "The total cost to cast Blasphemous Act is locked in before you pay that cost.")
    }
}
