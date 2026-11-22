package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Jace, Memory Adept — Magic 2012 #58 (canonical printing)
 * {3}{U}{U} · Legendary Planeswalker — Jace · Loyalty 4
 *
 * +1: Draw a card. Target player mills a card.
 * 0: Target player mills ten cards.
 * −7: Any number of target players each draw twenty cards.
 *
 * The −7 is "any number of target players" — a [TargetPlayer] with `unlimited = true` — iterated
 * with [Effects.ForEachTarget], each chosen player drawing via `Player.ContextPlayer(0)`.
 */
val JaceMemoryAdept = card("Jace, Memory Adept") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 4
    oracleText = "+1: Draw a card. Target player mills a card.\n" +
        "0: Target player mills ten cards.\n" +
        "−7: Any number of target players each draw twenty cards."

    loyaltyAbility(+1) {
        val player = target(Targets.Player)
        effect = Effects.DrawCards(1) then Patterns.Library.mill(1, player)
    }

    loyaltyAbility(0) {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(10, player)
    }

    loyaltyAbility(-7) {
        target(TargetPlayer(unlimited = true))
        effect = Effects.ForEachTarget(
            Effects.DrawCards(20, EffectTarget.PlayerRef(Player.ContextPlayer(0)))
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "58"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3f2a5b6-c26c-4355-b760-d87f074a4921.jpg?1783941097"
    }
}
