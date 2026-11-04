package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Orazca Relic — Rivals of Ixalan #181
 * {3} · Artifact
 *
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * {T}: Add {C}.
 * {T}, Sacrifice this artifact: You gain 3 life and draw a card. Activate only if you have the city's blessing.
 */
val OrazcaRelic = card("Orazca Relic") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "{T}: Add {C}.\n" +
        "{T}, Sacrifice this artifact: You gain 3 life and draw a card. Activate only if you have the city's blessing."

    keywords(Keyword.ASCEND)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.GainLife(3) then Effects.DrawCards(1)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.YouHaveCitysBlessing))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ed70bc5-8ea9-404e-a8cd-04322c8bc689.jpg?1783935265"
        ruling(
            "2018-01-19",
            "Once you have the city's blessing, you have it for the rest of the game, even if you lose control of some or all of your permanents. " +
                "The city's blessing isn't a permanent itself and can't be removed by any effect."
        )
        ruling(
            "2018-01-19",
            "A permanent is any object on the battlefield, including tokens and lands. Spells and emblems aren't permanents."
        )
    }
}
