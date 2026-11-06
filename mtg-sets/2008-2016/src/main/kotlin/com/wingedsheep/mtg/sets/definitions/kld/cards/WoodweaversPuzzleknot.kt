package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Woodweaver's Puzzleknot
 * {2}
 * Artifact
 *
 * When this artifact enters, you gain 3 life and get {E}{E}{E} (three energy counters).
 * {2}{G}, Sacrifice this artifact: You gain 3 life and get {E}{E}{E}.
 */
val WoodweaversPuzzleknot = card("Woodweaver's Puzzleknot") {
    manaCost = "{2}"
    colorIdentity = "G"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, you gain 3 life and get {E}{E}{E} (three energy counters).\n" +
        "{2}{G}, Sacrifice this artifact: You gain 3 life and get {E}{E}{E}."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3) then Effects.GetEnergy(3)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{G}"), Costs.SacrificeSelf)
        effect = Effects.GainLife(3) then Effects.GetEnergy(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "240"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/899d7420-f16d-4910-adf7-6deff47ecf1e.jpg?1783937145"
    }
}
