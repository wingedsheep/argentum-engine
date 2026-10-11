package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cut a Deal — {2}{W} Sorcery (Commander Legends: Battle for Baldur's Gate #14).
 *
 * Each opponent draws a card, then you draw a card for each opponent who drew a card this way.
 *
 * Modelling: an opponent "drew a card this way" exactly when their library held a card as the
 * spell resolved, so the count of opponents with a non-empty library is frozen before their
 * draws (the Aclazotz shape: [DynamicAmounts.countPlayersWith] rebinds "you" to each opponent),
 * then each opponent draws one, then you draw the frozen count.
 */
val CutADeal = card("Cut a Deal") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each opponent draws a card, then you draw a card for each opponent who drew a card this way."

    spell {
        effect = Effects.StoreNumber(
            "opponentsWhoDrew",
            DynamicAmounts.countPlayersWith(
                scope = Player.EachOpponent,
                condition = Conditions.CompareAmounts(
                    left = DynamicAmounts.count(Player.You, Zone.LIBRARY),
                    operator = ComparisonOperator.GTE,
                    right = 1,
                ),
            ),
        ) then
            Effects.DrawCards(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.DrawCards(DynamicAmounts.storedNumber("opponentsWhoDrew"))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Jodie Muir"
        flavorText = "\"It's a win-win arrangement. I just happen to win the most.\"\n—Duke Thalamra Vanthampur"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d846a4d-da81-41e0-89a9-6685c821dbb4.jpg?1783922817"
    }
}
