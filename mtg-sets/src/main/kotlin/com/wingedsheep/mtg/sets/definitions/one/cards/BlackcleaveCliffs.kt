package com.wingedsheep.mtg.sets.definitions.one.cards

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
 * Blackcleave Cliffs
 * Land
 *
 * This land enters tapped unless you control two or fewer other lands.
 * {T}: Add {B} or {R}.
 */
val BlackcleaveCliffs = card("Blackcleave Cliffs") {
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control two or fewer other lands.\n{T}: Add {B} or {R}."

    replacementEffect(EntersTapped(
        unlessCondition = Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            ComparisonOperator.LTE,
            DynamicAmount.Fixed(3)
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "248"
        artist = "Yeong-Hao Han"
        flavorText = "Where ambition meets industry, passion is poisoned by greed."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/1441fba4-fe06-4b5f-a103-aa6cf59a3859.jpg?1675957263"
        ruling("2023-02-04", "If one of these lands enters the battlefield at the same time as one or more other lands, it doesn't take those lands into consideration when determining how many other lands you control.")
        ruling("2023-02-04", "If one of these lands enters the battlefield under your control and you control zero, one, or two other lands, it enters the battlefield untapped. If you control three or more other lands, however, it enters the battlefield tapped.")
    }
}
