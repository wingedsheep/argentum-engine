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
 * Razorverge Thicket
 * Land
 *
 * This land enters tapped unless you control two or fewer other lands.
 * {T}: Add {G} or {W}.
 */
val RazorvergeThicket = card("Razorverge Thicket") {
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control two or fewer other lands.\n{T}: Add {G} or {W}."

    replacementEffect(EntersTapped(
        unlessCondition = Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            ComparisonOperator.LTE,
            DynamicAmount.Fixed(3)
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "257"
        artist = "Randy Gallegos"
        flavorText = "Where evolution meets austerity, primal might bucks against the yoke of hierarchy."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65b26f68-3a25-4c4e-bc76-a199ab479a50.jpg?1675957278"
        ruling("2023-02-04", "If one of these lands enters the battlefield at the same time as one or more other lands, it doesn't take those lands into consideration when determining how many other lands you control.")
        ruling("2023-02-04", "If one of these lands enters the battlefield under your control and you control zero, one, or two other lands, it enters the battlefield untapped. If you control three or more other lands, however, it enters the battlefield tapped.")
    }
}
