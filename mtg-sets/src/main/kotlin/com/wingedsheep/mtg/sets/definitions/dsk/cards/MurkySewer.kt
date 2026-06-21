package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Murky Sewer
 * Land
 *
 * This land enters tapped unless a player has 13 or less life.
 * {T}: Add {U} or {B}.
 */
val MurkySewer = card("Murky Sewer") {
    typeLine = "Land"
    colorIdentity = "UB"
    oracleText = "This land enters tapped unless a player has 13 or less life.\n{T}: Add {U} or {B}."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.APlayerLifeAtMost(13)
        )
    )

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
        rarity = Rarity.COMMON
        collectorNumber = "263"
        artist = "Martin de Diego Sádaba"
        flavorText = "They say dripping, hungry things writhe beyond the edge of the light, waiting to drag the unwary into the darkness."
        imageUri = "https://cards.scryfall.io/normal/front/6/0/6098d8be-4e3f-455d-8799-91435bf45a1c.jpg?1726286856"
    }
}
