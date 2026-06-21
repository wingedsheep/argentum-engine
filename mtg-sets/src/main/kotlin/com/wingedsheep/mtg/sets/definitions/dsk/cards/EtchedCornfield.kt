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
 * Etched Cornfield
 * Land
 *
 * This land enters tapped unless a player has 13 or less life.
 * {T}: Add {G} or {W}.
 */
val EtchedCornfield = card("Etched Cornfield") {
    typeLine = "Land"
    colorIdentity = "GW"
    oracleText = "This land enters tapped unless a player has 13 or less life.\n{T}: Add {G} or {W}."

    replacementEffect(
        EntersTapped(
            unlessCondition = Conditions.APlayerLifeAtMost(13)
        )
    )

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
        rarity = Rarity.COMMON
        collectorNumber = "258"
        artist = "Randy Gallegos"
        flavorText = "They say those who lose their way in the fields gradually wither and grow brittle, until at last they put down roots and anchor into the soil as a whispering stalk."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8900b89-0e10-4602-bba2-da8d60ea5885.jpg?1726286836"
    }
}
