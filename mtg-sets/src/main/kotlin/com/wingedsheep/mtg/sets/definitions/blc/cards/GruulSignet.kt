package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Gruul Signet
 * {2}
 * Artifact
 *
 * {1}, {T}: Add {R}{G}.
 */
val GruulSignet = card("Gruul Signet") {
    manaCost = "{2}"
    colorIdentity = "RG"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Add {R}{G}."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.Composite(
            Effects.AddMana(Color.RED),
            Effects.AddMana(Color.GREEN),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "273"
        artist = "Efrem Palacios"
        flavorText = "Gruul territorial markings need not be legible. The blood, snot, and muck used to smear them are unmistakably Gruul."
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e71ad66-28ae-4cd6-ac71-d8710e9ea9cd.jpg?1721975455"
    }
}
