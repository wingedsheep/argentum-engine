package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Maestros Charm
 * {U}{B}{R}
 * Instant
 * Choose one —
 * • Look at the top five cards of your library. Put one of those cards into your hand and the rest into your graveyard.
 * • Each opponent loses 3 life and you gain 3 life.
 * • Maestros Charm deals 5 damage to target creature or planeswalker.
 */
val MaestrosCharm = card("Maestros Charm") {
    manaCost = "{U}{B}{R}"
    colorIdentity = "BRU"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Look at the top five cards of your library. Put one of those cards into your hand and the rest into your graveyard.\n" +
        "• Each opponent loses 3 life and you gain 3 life.\n" +
        "• Maestros Charm deals 5 damage to target creature or planeswalker."

    spell {
        modal(chooseCount = 1) {
            mode("Look at the top five cards of your library. Put one of those cards into your hand and the rest into your graveyard") {
                effect = Patterns.Library.lookAtTopAndKeep(count = 5, keepCount = 1)
            }
            mode("Each opponent loses 3 life and you gain 3 life") {
                effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent)) then
                    Effects.GainLife(3)
            }
            mode("Maestros Charm deals 5 damage to target creature or planeswalker") {
                val t = target(Targets.CreatureOrPlaneswalker)
                effect = Effects.DealDamage(5, t)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "199"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3dcdedb6-3c24-4a29-b9b9-27cc47d8ee56.jpg"
    }
}
