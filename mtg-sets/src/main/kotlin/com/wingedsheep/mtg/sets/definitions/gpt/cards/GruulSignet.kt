package com.wingedsheep.mtg.sets.definitions.gpt.cards

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
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Greg Hildebrandt"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1bb30340-96a4-49d8-838b-8788900401a0.jpg?1593272924"
    }
}
