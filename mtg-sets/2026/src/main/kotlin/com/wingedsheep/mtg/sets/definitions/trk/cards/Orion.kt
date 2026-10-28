package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Orion
 * Land — Planet
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {R} or {G}.
 */
val Orion = card("Orion") {
    typeLine = "Land — Planet"
    colorIdentity = "GR"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {R} or {G}."
    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "288"
        artist = "Simon Dominic"
        flavorText = "\"Ensign's log: Whoa-oa-oa! I've never been banned from a whole planet before! I've gotta start more beefs with pirates!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/e/dec37ff4-d6e9-4b87-865b-125b17162465.jpg?1792632952"
    }
}

