package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Blazing Specter
 * {2}{B}{R}
 * Creature — Specter
 * 2/2
 * Flying, haste
 * Whenever Blazing Specter deals combat damage to a player, that player discards a card.
 */
val BlazingSpecter = card("Blazing Specter") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Specter"
    power = 2
    toughness = 2
    oracleText = "Flying, haste\nWhenever Blazing Specter deals combat damage to a player, that player discards a card."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = HandPatterns.discardCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "236"
        artist = "Marc Fishman"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bd397be-0e61-4f41-b0cf-f0c9d2440da7.jpg?1562907074"
    }
}
