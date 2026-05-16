package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.values.LandControllerScope

/**
 * Fellwar Stone
 * {2}
 * Artifact
 *
 * {T}: Add one mana of any color that a land an opponent controls could produce.
 */
val FellwarStone = card("Fellwar Stone") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color that a land an opponent controls could produce."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddManaOfColorLandsCouldProduce(LandControllerScope.OPPONENTS)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "269"
        artist = "John Avon"
        flavorText = "\"What do you have that I cannot obtain?\"\n—Mairsil, the Pretender"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e99c4fec-eb21-4288-a12f-1c58c4946bae.jpg?1721429553"
    }
}
