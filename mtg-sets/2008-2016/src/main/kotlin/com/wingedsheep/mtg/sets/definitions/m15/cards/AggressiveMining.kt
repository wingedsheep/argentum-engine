package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantPlayLands
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Aggressive Mining — Magic 2015 #127 (canonical printing)
 * {3}{R} · Enchantment
 *
 * You can't play lands.
 * Sacrifice a land: Draw two cards. Activate only once each turn.
 *
 * The lock is [PlayersCantPlayLands] scoped to the controller: it stops only the special action
 * of playing a land, so lands put onto the battlefield by a spell or ability still arrive. The
 * activated ability has no timing restriction, so `OncePerTurn` lets it be used once on each
 * player's turn, not just its controller's.
 */
val AggressiveMining = card("Aggressive Mining") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "You can't play lands.\nSacrifice a land: Draw two cards. Activate only once each turn."

    staticAbility { ability = PlayersCantPlayLands(Player.You) }

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Land)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.DrawCards(2)
        description = "Sacrifice a land: Draw two cards. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "127"
        artist = "Franz Vohwinkel"
        flavorText = "Designed by Markus Persson"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e15a3f1a-83af-4541-8d1c-bf7844f969c9.jpg?1783939177"
        ruling("2014-07-18", "You can activate the ability once on each player's turn, not just your own.")
        ruling("2014-07-18", "Aggressive Mining doesn't stop lands from being put onto the battlefield by a spell or ability. It only stops the special action of playing a land.")
    }
}
