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
 * Seachrome Coast
 * Land
 *
 * This land enters tapped unless you control two or fewer other lands.
 * {T}: Add {W} or {U}.
 */
val SeachromeCoast = card("Seachrome Coast") {
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control two or fewer other lands.\n{T}: Add {W} or {U}."

    replacementEffect(EntersTapped(
        unlessCondition = Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            ComparisonOperator.LTE,
            DynamicAmount.Fixed(3)
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "258"
        artist = "Mauricio Calle"
        flavorText = "Where indoctrination meets inquiry, currents roil restlessly beneath the surface."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ed7441f-f624-49c8-8611-d9bba0e441ac.jpg?1675957278"
        ruling("2023-02-04", "If one of these lands enters the battlefield at the same time as one or more other lands, it doesn't take those lands into consideration when determining how many other lands you control.")
        ruling("2023-02-04", "If one of these lands enters the battlefield under your control and you control zero, one, or two other lands, it enters the battlefield untapped. If you control three or more other lands, however, it enters the battlefield tapped.")
    }
}
