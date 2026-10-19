package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Wall of Vapor
 * {3}{U}
 * Creature — Wall
 * 0/1
 *
 * Defender (This creature can't attack.)
 * Prevent all damage that would be dealt to this creature by creatures it's blocking.
 *
 * A static [PreventDamage] with the live `blockedBySource()` source filter — see Wall of Shadows.
 */
val WallOfVapor = card("Wall of Vapor") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Wall"
    power = 0
    toughness = 1
    oracleText = "Defender (This creature can't attack.)\n" +
        "Prevent all damage that would be dealt to this creature by creatures it's blocking."

    keywords(Keyword.DEFENDER)

    replacementEffect(
        PreventDamage(
            amount = null,
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.Self,
                source = GameObjectFilter.Creature.blockedBySource(),
            )
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Richard Thomas"
        flavorText = "\"Walls of a castle are made out of stone,/ Walls of a house out of bricks or of wood./ " +
            "My walls are made out of magic alone,/ Stronger than any that ever have stood.\"\n" +
            "—Chrysoberyl Earthsdaughter, Incantations"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a6c0a27-d410-4ded-a842-70e1656ea21e.jpg?1783948070"
    }
}
