package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Cradle Clearcutter
 * {6}
 * Artifact Creature — Golem
 * 3/6
 * Prototype {2}{G} — 1/3
 * {T}: Add an amount of {G} equal to this creature's power.
 */
val CradleClearcutter = card("Cradle Clearcutter") {
    manaCost = "{6}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Golem"
    power = 3
    toughness = 6
    oracleText = "Prototype {2}{G} — 1/3 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "{T}: Add an amount of {G} equal to this creature's power."

    keywordAbility(KeywordAbility.prototype("{2}{G}", 1, 3))

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, DynamicAmounts.sourcePower())
        manaAbility = true
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Daarken"
        flavorText = "A thousand years of growth, harvested in minutes."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50cc9b90-792e-4cf7-ab8c-cb616d94092a.jpg?1783920035"
    }
}
