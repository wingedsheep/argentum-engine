package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Shortcut Seeker
 * {3}{U}
 * Creature — Human Rogue
 * 2/5
 * Whenever this creature deals combat damage to a player, venture into the dungeon.
 * (Enter the first room or advance to the next room.)
 */
val ShortcutSeeker = card("Shortcut Seeker") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    oracleText = "Whenever this creature deals combat damage to a player, venture into the dungeon. (Enter the first room or advance to the next room.)"
    power = 2
    toughness = 5

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Artur Treffner"
        flavorText = "\"Ah, the old 'trap door under a rug' trick. This is almost too easy.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/2/122d2635-c4f4-44cd-8f82-37e3016e421e.jpg?1783926508"
    }
}
