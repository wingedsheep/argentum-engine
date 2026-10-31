package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.NoMaximumHandSize
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Reliquary Tower
 * Land
 *
 * You have no maximum hand size.
 * {T}: Add {C}.
 */
val ReliquaryTower = card("Reliquary Tower") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "You have no maximum hand size.\n{T}: Add {C}."

    staticAbility {
        ability = NoMaximumHandSize()
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "143"
        artist = "Jesper Ejsing"
        flavorText = "When the aven scouts located the tower in Esper, the Knights of the Reliquary set off on a righteous crusade to recover their lost treasures."
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5c0c1a5-dce7-4c7d-8a5b-0bf93ba68ace.jpg?1562803667"
    }
}
