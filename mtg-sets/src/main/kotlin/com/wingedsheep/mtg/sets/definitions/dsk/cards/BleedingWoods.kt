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
 * Bleeding Woods
 * Land
 *
 * This land enters tapped unless a player has 13 or less life.
 * {T}: Add {R} or {G}.
 */
val BleedingWoods = card("Bleeding Woods") {
    typeLine = "Land"
    colorIdentity = "RG"
    oracleText = "This land enters tapped unless a player has 13 or less life.\n{T}: Add {R} or {G}."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.APlayerLifeAtMost(13)
        )
    )

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "257"
        artist = "Henry Peters"
        flavorText = "They say if someone lies down to sleep beneath the trees, in the morning nothing will remain but a few scattered bones and a bed of blood-red lilies."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb224874-aff5-461f-82ee-89b06663231a.jpg?1726286833"
    }
}
