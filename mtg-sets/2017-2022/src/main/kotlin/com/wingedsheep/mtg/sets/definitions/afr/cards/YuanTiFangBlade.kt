package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Yuan-Ti Fang-Blade
 * {2}{B}
 * Creature — Snake Rogue
 * 2/2
 * Deathtouch
 * Whenever this creature deals combat damage to a player, venture into the dungeon.
 * (Enter the first room or advance to the next room.)
 */
val YuanTiFangBlade = card("Yuan-Ti Fang-Blade") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Snake Rogue"
    oracleText = "Deathtouch\nWhenever this creature deals combat damage to a player, venture into the dungeon. (Enter the first room or advance to the next room.)"
    power = 2
    toughness = 2

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.VentureIntoTheDungeon()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Simon Dominic"
        flavorText = "\"The air is sweet with the taste of your fear.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7e0e09c-ed43-4a37-85f2-c60bdfdb2624.jpg?1783926485"
    }
}
