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
 * Arch of Orazca — Rivals of Ixalan #185
 * Land
 *
 * Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)
 * {T}: Add {C}.
 * {5}, {T}: Draw a card. Activate only if you have the city's blessing.
 */
val ArchOfOrazca = card("Arch of Orazca") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land"
    oracleText = "Ascend (If you control ten or more permanents, you get the city's blessing for the rest of the game.)\n" +
        "{T}: Add {C}.\n" +
        "{5}, {T}: Draw a card. Activate only if you have the city's blessing."

    keywords(Keyword.ASCEND)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = Effects.DrawCards(1)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.YouHaveCitysBlessing))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "185"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6d47162-749b-47d5-9589-8f1dbf60b9f3.jpg?1783935263"
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
