package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dragon Mage
 * {5}{R}{R}
 * Creature — Dragon Wizard
 * 5/5
 * Flying
 * Whenever Dragon Mage deals combat damage to a player, each player discards their hand, then draws seven cards.
 */
val DragonMage = card("Dragon Mage") {
    manaCost = "{5}{R}{R}"
    typeLine = "Creature — Dragon Wizard"
    power = 5
    toughness = 5
    oracleText = "Flying\nWhenever Dragon Mage deals combat damage to a player, each player discards their hand, then draws seven cards."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = ForEachPlayerEffect(
            players = Player.Each,
            effects = listOf(
                GatherCardsEffect(CardSource.FromZone(Zone.HAND, Player.You), storeAs = "discardedHand"),
                MoveCollectionEffect("discardedHand", CardDestination.ToZone(Zone.GRAVEYARD, Player.You), moveType = MoveType.Discard),
                DrawCardsEffect(7)
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Matthew D. Wilson"
        flavorText = "\"You'll bend to my will—with or without your precious sanity.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/6/7687a201-0ecc-4739-86e3-3b4090d345a8.jpg?1562530729"
    }
}
