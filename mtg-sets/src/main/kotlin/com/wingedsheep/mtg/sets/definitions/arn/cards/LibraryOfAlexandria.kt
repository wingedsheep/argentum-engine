package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Library of Alexandria
 * Land
 * {T}: Add {C}.
 * {T}: Draw a card. Activate only if you have exactly seven cards in hand.
 */
val LibraryOfAlexandria = card("Library of Alexandria") {
    typeLine = "Land"
    colorIdentity = ""
    oracleText = "{T}: Add {C}.\n{T}: Draw a card. Activate only if you have exactly seven cards in hand."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.DrawCards(1)
        restrictions = listOf(
            ActivationRestriction.All(
                listOf(
                    ActivationRestriction.OnlyIfCondition(Conditions.CardsInHandAtLeast(7)),
                    ActivationRestriction.OnlyIfCondition(Conditions.CardsInHandAtMost(7)),
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee266113-34ce-4189-84e7-ee2c86a2722c.jpg?1562939686"
    }
}
