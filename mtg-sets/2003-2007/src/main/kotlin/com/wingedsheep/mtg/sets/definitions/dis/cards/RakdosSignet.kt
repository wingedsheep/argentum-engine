package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Rakdos Signet
 * {2}
 * Artifact
 * {1}, {T}: Add {B}{R}.
 */
val RakdosSignet = card("Rakdos Signet") {
    manaCost = "{2}"
    colorIdentity = "BR"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Add {B}{R}."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.AddMana(Color.BLACK) then Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "Greg Hildebrandt"
        flavorText = "Made of bone and boiled in blood, a Rakdos signet is not considered finished until it has been used as a murder weapon."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17a3f95c-5a05-46ea-8dc6-b77b59324035.jpg?1783943381"
    }
}
