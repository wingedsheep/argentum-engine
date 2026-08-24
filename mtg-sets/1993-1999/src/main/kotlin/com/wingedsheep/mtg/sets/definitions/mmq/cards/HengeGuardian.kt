package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Henge Guardian
 * {5}
 * Artifact Creature — Dragon Wurm
 * 3 / 4
 */
val HengeGuardian = card("Henge Guardian") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Dragon Wurm"
    oracleText = "{2}: This creature gains trample until end of turn."
    power = 3
    toughness = 4

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "297"
        artist = "Chippy"
        flavorText = "Like so many Thran relics, the wurm engine kept operating long after its creators were gone."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/028e5e18-b639-4461-87e4-5306371440b5.jpg"
    }
}
