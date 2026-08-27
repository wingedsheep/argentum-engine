package com.wingedsheep.mtg.sets.definitions.apc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sylvan Messenger
 * {3}{G}
 * Creature — Elf
 * 2/2
 * Trample
 * When this creature enters, reveal the top four cards of your library. Put all Elf cards revealed
 * this way into your hand and the rest on the bottom of your library in any order.
 */
val SylvanMessenger = card("Sylvan Messenger") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf"
    power = 2
    toughness = 2
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\nWhen this creature enters, reveal the top four cards of your library. Put all Elf cards revealed this way into your hand and the rest on the bottom of your library in any order."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.revealTopPutAllMatchingToHand(
            count = DynamicAmount.Fixed(4),
            filter = GameObjectFilter.Permanent.withSubtype(Subtype.ELF),
            restOrder = CardOrder.ControllerChooses,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd67d17e-23d2-47a0-a10b-c3d63cbf969a.jpg?1783945339"
    }
}
