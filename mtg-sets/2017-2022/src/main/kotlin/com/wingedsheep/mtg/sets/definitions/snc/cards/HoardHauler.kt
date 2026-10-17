package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hoard Hauler
 * {3}{R}
 * Artifact — Vehicle
 * 5/5
 *
 * Trample
 * Whenever this Vehicle deals combat damage to a player, create a Treasure token for each artifact
 * they control.
 * Crew 3
 *
 * "They" is the damaged player: [Player.TriggeringPlayer] on a combat-damage trigger. The count is
 * read at resolution (the Overencumbered / Keeper of the Accord battlefield-count shape).
 */
val HoardHauler = card("Hoard Hauler") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Vehicle"
    power = 5
    toughness = 5
    oracleText = "Trample\n" +
        "Whenever this Vehicle deals combat damage to a player, create a Treasure token for each artifact they control.\n" +
        "Crew 3 (Tap any number of creatures you control with total power 3 or more: This Vehicle becomes an artifact creature until end of turn.)"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateTreasure(
            count = DynamicAmounts.battlefield(Player.TriggeringPlayer, GameObjectFilter.Artifact).count()
        )
    }

    keywordAbility(KeywordAbility.crew(3))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Jake Murray"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19f190e9-52f3-4e34-8fb6-88174c189ee7.jpg?1783923119"
    }
}
