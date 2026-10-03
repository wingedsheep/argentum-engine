package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Refurbished Familiar
 * {3}{B}
 * Artifact Creature — Zombie Rat
 * 2/1
 * Affinity for artifacts
 * Flying
 * When this creature enters, each opponent discards a card. For each opponent who can't, you
 * draw a card.
 *
 * The draw counts opponents with an empty hand (the only way a player "can't" discard a card),
 * snapshotted before the discards — the Aclazotz idiom. Discards never change another player's
 * hand, so counting first is outcome-equivalent to the printed discard-then-draw order.
 */
val RefurbishedFamiliar = card("Refurbished Familiar") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Zombie Rat"
    power = 2
    toughness = 1
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "Flying\n" +
        "When this creature enters, each opponent discards a card. For each opponent who can't, you draw a card."

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(
            DynamicAmounts.countPlayersWith(
                scope = Player.EachOpponent,
                condition = Conditions.CompareAmounts(
                    left = DynamicAmounts.cardsInYourHand(),
                    operator = ComparisonOperator.LTE,
                    right = 0,
                ),
            )
        ) then
            Effects.EachOpponentDiscards(1)
        description = "When this creature enters, each opponent discards a card. For each " +
            "opponent who can't, you draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Steve Ellis"
        flavorText = "When her beloved pet died, the artificer turned to necromancy."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b338e078-629c-4cac-bd1d-e1f0a132728d.jpg?1783911277"
    }
}
