package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Inspired Ultimatum
 * {U}{U}{R}{R}{R}{W}{W}
 * Sorcery
 *
 * Target player gains 5 life, Inspired Ultimatum deals 5 damage to any target, then you draw
 * five cards.
 *
 * Two independent single-slot targets — the player and "any target" may be the same player.
 * If one becomes illegal, the other is still affected and you still draw; if both do, the
 * spell doesn't resolve.
 */
val InspiredUltimatum = card("Inspired Ultimatum") {
    manaCost = "{U}{U}{R}{R}{R}{W}{W}"
    colorIdentity = "RUW"
    typeLine = "Sorcery"
    oracleText = "Target player gains 5 life, Inspired Ultimatum deals 5 damage to any target, " +
        "then you draw five cards."

    spell {
        val player = target(Targets.Player)
        val anyTarget = target(Targets.Any)
        effect = Effects.GainLife(5, player) then
            Effects.DealDamage(5, anyTarget) then
            Effects.DrawCards(5)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Tyler Jacobson"
        flavorText = "\"Inner peace is wonderful, and worth striving for. Outer peace is much less important.\"\n" +
            "—Taelya, Lavabrink pyromage"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd64f064-8f05-41ef-b95b-1b723137f846.jpg?1783931024"
        ruling("2020-04-17", "You can target yourself as the player to gain 5 life.")
        ruling("2020-04-17", "If either target becomes illegal before Inspired Ultimatum resolves, the other will be affected as appropriate and you'll draw five cards. If both targets become illegal, the spell won't resolve and you won't draw five cards.")
    }
}
