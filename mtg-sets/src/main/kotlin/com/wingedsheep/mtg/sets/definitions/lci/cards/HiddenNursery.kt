package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Hidden Nursery
 * Land — Cave
 * This land enters tapped.
 * {T}: Add {G}.
 * {4}{G}, {T}, Sacrifice this land: Discover 4. Activate only as a sorcery.
 */
val HiddenNursery = card("Hidden Nursery") {
    typeLine = "Land — Cave"
    colorIdentity = "G"
    oracleText = "This land enters tapped.\n{T}: Add {G}.\n{4}{G}, {T}, Sacrifice this land: Discover 4. Activate only as a sorcery."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{G}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Discover(4)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "276"
        artist = "Álvaro Calvo Escudero"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a942939a-c06e-4b90-a404-ae5acfffcff9.jpg?1782694391"
    }
}
