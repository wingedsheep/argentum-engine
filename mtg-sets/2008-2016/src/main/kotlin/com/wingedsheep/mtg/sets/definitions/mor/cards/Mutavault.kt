package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mutavault — Morningtide #148
 * Land
 * {T}: Add {C}.
 * {1}: This land becomes a 2/2 creature with all creature types until end of turn. It's still a land.
 *
 * "All creature types" rides on a granted Changeling keyword, which the layer system expands to
 * every creature type (the Faceless Haven / Soulstone Sanctuary shape). BecomeCreature keeps the
 * Land type, so it's still a land.
 */
val Mutavault = card("Mutavault") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}: This land becomes a 2/2 creature with all creature types until end of turn. It's still a land."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.BecomeCreature(
            target = EffectTarget.Self,
            power = 2,
            toughness = 2,
            keywords = setOf(Keyword.CHANGELING),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Fred Fields"
        flavorText = "Some changelings born at Velis Vel never return, but their essence never leaves."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8ca3c48b-f104-4292-9a4e-2ce87a65893c.jpg?1783942774"
        ruling(
            "2025-07-25",
            "If this land becomes a creature but you haven't controlled it continuously since your most " +
                "recent turn began, you won't be able to activate its mana ability or attack with it that turn."
        )
    }
}
