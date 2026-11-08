package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Glassblower's Puzzleknot
 * {2}
 * Artifact
 * When this artifact enters, scry 2, then you get {E}{E}.
 * {2}{U}, Sacrifice this artifact: Scry 2, then you get {E}{E}.
 */
val GlassblowersPuzzleknot = card("Glassblower's Puzzleknot") {
    manaCost = "{2}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, scry 2, then you get {E}{E}. (You get two energy counters. " +
        "To scry 2, look at the top two cards of your library, then put any number of them on the bottom " +
        "and the rest on top in any order.)\n" +
        "{2}{U}, Sacrifice this artifact: Scry 2, then you get {E}{E}."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(2) then Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.SacrificeSelf)
        effect = Effects.Scry(2) then Effects.GetEnergy(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "217"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d123031-6702-4d49-a788-42eb6dbc569e.jpg?1783937154"
    }
}
