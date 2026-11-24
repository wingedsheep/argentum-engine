package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Grove of the Guardian
 * Land
 * {T}: Add {C}.
 * {3}{G}{W}, {T}, Tap two untapped creatures you control, Sacrifice this land: Create an 8/8 green
 * and white Elemental creature token with vigilance.
 */
val GroveOfTheGuardian = card("Grove of the Guardian") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{3}{G}{W}, {T}, Tap two untapped creatures you control, Sacrifice this land: " +
        "Create an 8/8 green and white Elemental creature token with vigilance."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{G}{W}"),
            Costs.Tap,
            Costs.TapPermanents(2),
            Costs.SacrificeSelf
        )
        effect = Effects.CreateToken(
            power = 8,
            toughness = 8,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Elemental"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/b/6/b64c5f80-4676-4860-be0e-20bcf2227405.jpg?1783940381"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "240"
        artist = "Christine Choi"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3cf60ca0-e01f-499c-8d04-d59050f38c33.jpg?1783940321"

        ruling(
            "2012-10-01",
            "To activate Grove of the Guardian's second ability you can tap any two untapped creatures you " +
                "control, including ones you haven't controlled continuously since the beginning of your most recent turn."
        )
    }
}
