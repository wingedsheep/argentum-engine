package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Akki Ronin — Kamigawa: Neon Dynasty #131 (canonical printing)
 * {1}{R} · Creature — Goblin Samurai · 1/3
 *
 * Whenever a Samurai or Warrior you control attacks alone, you may discard a card.
 * If you do, draw a card.
 */
val AkkiRonin = card("Akki Ronin") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Samurai"
    power = 1
    toughness = 3
    oracleText = "Whenever a Samurai or Warrior you control attacks alone, you may discard a card. If you do, draw a card."

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.SAMURAI, Subtype.WARRIOR)).youControl()
        ).attacks(setOf(AttackPredicate.Alone))
        effect = Effects.May(
            Effects.IfYouDo(
                action = Effects.Discard(1),
                then = Effects.DrawCards(1)
            )
        )
        description = "Whenever a Samurai or Warrior you control attacks alone, you may discard a card. " +
            "If you do, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "131"
        artist = "Olivier Bernard"
        flavorText = "\"Carry the heat of the furnace in your steel, and never let it fade.\"\n—Keni-Keni, akki smith"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96f15754-1ca6-45b0-a7de-2fe507df2a1a.jpg?1783923872"
    }
}
