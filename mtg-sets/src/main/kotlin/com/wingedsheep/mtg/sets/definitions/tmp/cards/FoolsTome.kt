package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Fool's Tome
 * {4}
 * Artifact — Book
 *
 * {2}, {T}: Draw a card. Activate only if you have no cards in hand.
 */
val FoolsTome = card("Fool's Tome") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact — Book"
    oracleText = "{2}, {T}: Draw a card. Activate only if you have no cards in hand."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.Tap
        )
        effect = Effects.DrawCards(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.EmptyHand)
        )
        description = "{2}, {T}: Draw a card. Activate only if you have no cards in hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "289"
        artist = "Julie Baroh"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83be257c-8945-46be-8b58-fb2881084026.jpg?1562054974"
    }
}
