package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ReduceEquipCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Nahiri, Storm of Stone — War of the Spark #233 (canonical printing)
 * {2}{R/W}{R/W} · Legendary Planeswalker — Nahiri · Starting Loyalty: 6
 *
 * During your turn, creatures you control have first strike and equip abilities you activate
 * cost {1} less to activate.
 * −X: Nahiri deals X damage to target tapped creature.
 *
 * The static is two your-turn [Conditions.IsYourTurn]-gated statics (Gideon Blackblade's shape):
 * a first-strike grant over creatures you control, and Éowyn's controller-scoped
 * [ReduceEquipCost], whose condition the equip-cost path evaluates at activation.
 */
val NahiriStormOfStone = card("Nahiri, Storm of Stone") {
    manaCost = "{2}{R/W}{R/W}"
    colorIdentity = "RW"
    typeLine = "Legendary Planeswalker — Nahiri"
    startingLoyalty = 6
    oracleText = "During your turn, creatures you control have first strike and equip abilities you " +
        "activate cost {1} less to activate.\n" +
        "−X: Nahiri deals X damage to target tapped creature."

    // "During your turn, creatures you control have first strike ..."
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = GrantKeyword(Keyword.FIRST_STRIKE, GroupFilter.AllCreaturesYouControl)
    }
    // "... and equip abilities you activate cost {1} less to activate."
    staticAbility {
        condition = Conditions.IsYourTurn
        ability = ReduceEquipCost(1)
    }

    // −X: Nahiri deals X damage to target tapped creature.
    loyaltyAbilityX {
        val creature = target(TargetFilter.TappedCreature)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/8806605c-0cb7-4360-96e4-afb424ccad90.jpg?1783933377"
    }
}
