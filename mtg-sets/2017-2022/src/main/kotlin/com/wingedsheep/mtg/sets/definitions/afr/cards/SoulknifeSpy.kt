package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Soulknife Spy
 * {2}{U}
 * Creature — Elf Rogue
 * 3/2
 * Whenever this creature deals combat damage to a player, draw a card.
 */
val SoulknifeSpy = card("Soulknife Spy") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elf Rogue"
    oracleText = "Whenever this creature deals combat damage to a player, draw a card."
    power = 3
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Miguel Mercado"
        flavorText = "\"My psychic blade cuts the mind and severs thoughts. Even if you survive, you'll never remember I was here.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/4/045c7c23-d418-42f0-b497-50c39e5321ad.jpg?1783926507"
    }
}
