package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Liliana's Reaver
 * {2}{B}{B}
 * Creature — Zombie
 * 4/3
 * Deathtouch
 * Whenever this creature deals combat damage to a player, that player discards a card and you
 * create a tapped 2/2 black Zombie creature token.
 */
val LilianasReaver = card("Liliana's Reaver") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 4
    toughness = 3
    oracleText = "Deathtouch\nWhenever this creature deals combat damage to a player, that player discards a card " +
        "and you create a tapped 2/2 black Zombie creature token."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Hand.discardCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
                tapped = true
            )
        description = "Whenever this creature deals combat damage to a player, that player discards a card " +
            "and you create a tapped 2/2 black Zombie creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "103"
        artist = "Karl Kopinski"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a734c33c-4fa0-4f7a-943c-14a8aecea1a6.jpg?1783939922"
    }
}
