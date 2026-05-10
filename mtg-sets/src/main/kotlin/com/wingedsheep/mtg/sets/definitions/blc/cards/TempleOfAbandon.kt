package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddManaEffect

/**
 * Temple of Abandon
 * Land
 *
 * This land enters tapped.
 * When this land enters, scry 1. (Look at the top card of your library. You may put that card on the bottom.)
 * {T}: Add {R} or {G}.
 */
val TempleOfAbandon = card("Temple of Abandon") {
    typeLine = "Land"
    colorIdentity = "RG"
    oracleText = "This land enters tapped.\nWhen this land enters, scry 1. (Look at the top card of your library. You may put that card on the bottom.)\n{T}: Add {R} or {G}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.scry(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "338"
        artist = "Mike Bierek"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59866586-8a4c-464e-8273-2ec06f29d28e.jpg?1721429868"
    }
}
