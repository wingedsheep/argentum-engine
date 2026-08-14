package com.wingedsheep.mtg.sets.definitions.som.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Darkslick Shores
 * Land
 *
 * This land enters tapped unless you control two or fewer other lands.
 * {T}: Add {U} or {B}.
 */
val DarkslickShores = card("Darkslick Shores") {
    typeLine = "Land"
    colorIdentity = "UB"
    oracleText = "This land enters tapped unless you control two or fewer other lands.\n{T}: Add {U} or {B}."

    replacementEffect(EntersTapped(
        unlessCondition = Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            ComparisonOperator.LTE,
            DynamicAmount.Fixed(3)
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Charles Urbach"
        flavorText = "Where the Mephidross leaches into the Quicksilver Sea, the waves writhe with necrogen."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e530388b-eb19-4211-abd8-8a4c3c38c3af.jpg?1783941689"
        ruling("2023-02-04", "If one of these lands enters the battlefield under your control and you control zero, one, or two other lands, it enters the battlefield untapped. If you control three or more other lands, it enters the battlefield tapped.")
        ruling("2023-02-04", "If one of these lands enters the battlefield at the same time as one or more other lands (due to Oblivion Sower or Warp World, perhaps), it doesn't take those lands into consideration when determining how many other lands you control.")
    }
}
