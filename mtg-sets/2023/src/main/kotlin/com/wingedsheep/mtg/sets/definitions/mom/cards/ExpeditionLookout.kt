package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Expedition Lookout
 * {1}{U}
 * Creature — Merfolk Rogue
 * 2/3
 * Defender
 * As long as an opponent has eight or more cards in their graveyard, this creature can attack
 * as though it didn't have defender and it can't be blocked.
 *
 * "An opponent has eight or more" is per-opponent, so the gate is the greatest graveyard size
 * among opponents (each measured from their own point of view), not their summed total.
 */
private val OpponentHasEightInGraveyard = Conditions.CompareAmounts(
    DynamicAmounts.greatestAmongPlayers(
        DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
        Player.EachOpponent
    ),
    ComparisonOperator.GTE,
    8
)

val ExpeditionLookout = card("Expedition Lookout") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Rogue"
    oracleText = "Defender\nAs long as an opponent has eight or more cards in their graveyard, this creature can attack as though it didn't have defender and it can't be blocked."
    power = 2
    toughness = 3

    keywords(Keyword.DEFENDER)

    staticAbility {
        ability = CanAttackDespiteDefender(condition = OpponentHasEightInGraveyard)
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = OpponentHasEightInGraveyard
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "56"
        artist = "Johan Grenier"
        flavorText = "Skills honed on perilous treasure hunts helped Zendikar's adventurers stay a crucial step ahead of their Phyrexian foes."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e14dfd2-5805-46ef-bd19-dab7ac23fbce.jpg?1783917037"
    }
}
