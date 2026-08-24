package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Havenwood Battleground
 * Land
 * This land enters tapped.
 * {T}: Add {G}.
 * {T}, Sacrifice this land: Add {G}{G}.
 *
 * The green member of the sacrifice-land cycle — see [DwarvenRuins].
 */
val HavenwoodBattleground = card("Havenwood Battleground") {
    colorIdentity = "G"
    typeLine = "Land"
    oracleText = "This land enters tapped.\n{T}: Add {G}.\n{T}, Sacrifice this land: Add {G}{G}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddMana(Color.GREEN)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        manaAbility = true
        effect = Effects.AddMana(Color.GREEN, 2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/9028f200-80dd-4c53-877f-ea380ff417cb.jpg?1783947879"
    }
}
