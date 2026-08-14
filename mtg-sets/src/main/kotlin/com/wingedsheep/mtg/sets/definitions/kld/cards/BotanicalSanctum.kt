package com.wingedsheep.mtg.sets.definitions.kld.cards

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
 * Botanical Sanctum
 * Land
 *
 * This land enters tapped unless you control two or fewer other lands.
 * {T}: Add {G} or {U}.
 */
val BotanicalSanctum = card("Botanical Sanctum") {
    typeLine = "Land"
    colorIdentity = "GU"
    oracleText = "This land enters tapped unless you control two or fewer other lands.\n{T}: Add {G} or {U}."

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
        effect = AddManaEffect(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Christine Choi"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8744471b-a528-47d9-84d0-4526273f55e9.jpg?1783937144"
        ruling("2016-09-20", "If one of these lands is your first, second, or third land, it enters the battlefield untapped. If you control three or more other lands, however, it enters the battlefield tapped.")
        ruling("2016-09-20", "If one of these lands enters the battlefield at the same time as one or more other lands (due to Oblivion Sower or Warp World, perhaps), it doesn't take those lands into consideration when determining how many other lands you control.")
    }
}
