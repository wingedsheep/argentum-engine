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
 * Dreamroot Cascade
 * Land
 *
 * This land enters tapped unless you control two or more other lands.
 * {T}: Add {G} or {U}.
 *
 * One of the "slow lands" — enters untapped only once you already control at least
 * two other lands. The [GameObjectFilter.Land] aggregate counts the entering land
 * itself, so "two or more *other* lands" is "three or more lands total", i.e. the
 * untapped condition is `controlled lands >= 3`.
 */
val DreamrootCascade = card("Dreamroot Cascade") {
    typeLine = "Land"
    colorIdentity = "GU"
    oracleText = "This land enters tapped unless you control two or more other lands.\n{T}: Add {G} or {U}."

    replacementEffect(EntersTapped(
        unlessCondition = Compare(
            DynamicAmount.AggregateBattlefield(Player.You, GameObjectFilter.Land),
            ComparisonOperator.GTE,
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
        collectorNumber = "262"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb604455-c411-414d-a2ef-e7567ee86a4d.jpg?1655879797"
        flavorText = "Soothing whispers invite travelers to step closer, closer, closer . . . until they vanish soundlessly beneath the surface."
        ruling("2021-11-19", "If one of these lands enters the battlefield at the same time as one or more other lands, it doesn't take those lands into consideration when determining how many other lands you control.")
    }
}
