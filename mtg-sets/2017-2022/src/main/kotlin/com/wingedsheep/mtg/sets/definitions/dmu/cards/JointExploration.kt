package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Joint Exploration
 * {1}{U}
 * Instant
 * Kicker {G}
 * Scry 2, then draw a card. If this spell was kicked, you may put a land card from your hand
 * onto the battlefield.
 */
val JointExploration = card("Joint Exploration") {
    manaCost = "{1}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Kicker {G} (You may pay an additional {G} as you cast this spell.)\nScry 2, then draw a card. If this spell was kicked, you may put a land card from your hand onto the battlefield."

    keywordAbility(KeywordAbility.kicker("{G}"))

    spell {
        effect = Effects.Scry(2) then Effects.DrawCards(1) then Effects.If(
            Conditions.WasKicked,
            Patterns.Hand.putFromHand(filter = GameObjectFilter.Land)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Zezhou Chen"
        flavorText = "The Tolarians had the maps, but only the Yavimayans knew how to read them."
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b25ab334-1060-457e-b212-3d7a697c545e.jpg?1783921348"
    }
}
