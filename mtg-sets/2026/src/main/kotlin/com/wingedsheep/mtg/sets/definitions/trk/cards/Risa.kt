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
 * Risa
 * Land — Planet
 *
 * This land enters tapped.
 * When this land enters, you gain 1 life.
 * {T}: Add {G} or {W}.
 */
val Risa = card("Risa") {
    typeLine = "Land — Planet"
    colorIdentity = "GW"
    oracleText = "This land enters tapped.\nWhen this land enters, you gain 1 life.\n{T}: Add {G} or {W}."
    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(1)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "293"
        artist = "Colin Searle"
        flavorText = "\"Lieutenant junior grade's log: We're approaching Risa for some much needed shore leave. I just picked up this old Todd Matthews detective novel. I bet it's great!\""
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c89e7680-9868-4fea-9aaf-6af03bfa0692.jpg?1792632968"
    }
}

