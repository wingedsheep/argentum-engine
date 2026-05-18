package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Charcoal Diamond
 * {2}
 * Artifact
 *
 * Charcoal Diamond enters tapped.
 * {T}: Add {B}.
 */
val CharcoalDiamond = card("Charcoal Diamond") {
    manaCost = "{2}"
    typeLine = "Artifact"
    colorIdentity = "B"
    oracleText = "This artifact enters tapped.\n{T}: Add {B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "296"
        artist = "Drew Tucker"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a81b3f1-babc-4bd7-8b87-754c8389ae85.jpg?1562718326"
    }
}
