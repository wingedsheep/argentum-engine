package com.wingedsheep.mtg.sets.definitions.vow.cards

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
 * Sundown Pass
 * Land
 *
 * This land enters tapped unless you control two or more other lands.
 * {T}: Add {R} or {W}.
 *
 * One of the "slow lands" — enters untapped only once you already control at least
 * two other lands. The [GameObjectFilter.Land] aggregate counts the entering land
 * itself, so "two or more *other* lands" is "three or more lands total", i.e. the
 * untapped condition is `controlled lands >= 3`.
 */
val SundownPass = card("Sundown Pass") {
    typeLine = "Land"
    colorIdentity = "RW"
    oracleText = "This land enters tapped unless you control two or more other lands.\n{T}: Add {R} or {W}."

    replacementEffect(EntersTapped(
        unlessCondition = Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            ComparisonOperator.GTE,
            DynamicAmount.Fixed(3)
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
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
        collectorNumber = "266"
        artist = "Muhammad Firdaus"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f3fddd7-ede4-41c7-a645-a6af298a3d35.jpg?1655879812"
        flavorText = "Riders chase the fading light recklessly through the mountains, desperate for one last glimpse of day."
        ruling("2021-11-19", "If one of these lands enters the battlefield at the same time as one or more other lands, it doesn't take those lands into consideration when determining how many other lands you control.")
    }
}
