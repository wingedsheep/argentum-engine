package com.wingedsheep.mtg.sets.definitions.usg.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Windfall
 * {2}{U}
 * Sorcery
 * Each player discards their hand, then draws cards equal to the greatest number of cards a player
 * discarded this way.
 *
 * Wheel of Fortune's two-pass shape with a captured draw count. Every card in a hand is discarded
 * by "discards their hand", so the number a player discards this way is their hand size at
 * resolution: the greatest hand size among all players is frozen with [Effects.StoreNumber]
 * *before* anyone discards (afterwards every hand reads 0), then every player discards their hand,
 * then every player draws that many. [DynamicAmounts.greatestAmongPlayers] evaluates
 * `cardsInYourHand()` as each player in turn, and `ForEachPlayer` keeps outer stored numbers, so
 * each iteration of the draw pass reads the same frozen value.
 */
val Windfall = card("Windfall") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Each player discards their hand, then draws cards equal to the greatest number " +
        "of cards a player discarded this way."

    spell {
        effect = Effects.StoreNumber(
            "windfallGreatestDiscard",
            DynamicAmounts.greatestAmongPlayers(DynamicAmounts.cardsInYourHand(), Player.Each)
        ) then
            Effects.ForEachPlayer(Player.Each, Patterns.Hand.discardHand()) then
            Effects.ForEachPlayer(
                Player.Each,
                Effects.DrawCards(DynamicAmounts.storedNumber("windfallGreatestDiscard"))
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Pete Venters"
        flavorText = "\"To fill your mind with knowledge, we must start by emptying it.\"\n—Barrin, master wizard"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2aef4608-5ba8-4636-b5e7-cac57c5c0608.jpg?1783946351"
    }
}
