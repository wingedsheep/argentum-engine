package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Sol Ring
 * {1}
 * Artifact
 *
 * {T}: Add {C}{C}.
 */
val SolRing = card("Sol Ring") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}{C}."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "269"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4300d24-1cae-4dd5-be7e-38cc677cf5bd.jpg?1559591399"
    }
}
