package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate

/**
 * Peerless Samurai — Kamigawa: Neon Dynasty #156 (canonical printing)
 * {2}{R} · Creature — Human Samurai · 2/3
 *
 * Menace
 * Whenever a Samurai or Warrior you control attacks alone, the next spell you cast this turn
 * costs {1} less to cast.
 *
 * The discount is a one-shot rider on the game state: the next spell cast this turn spends it
 * whether or not it had generic mana to reduce.
 */
val PeerlessSamurai = card("Peerless Samurai") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Samurai"
    power = 2
    toughness = 3
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever a Samurai or Warrior you control attacks alone, the next spell you cast this " +
        "turn costs {1} less to cast."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.SAMURAI, Subtype.WARRIOR)).youControl()
        ).attacks(setOf(AttackPredicate.Alone))
        effect = Effects.ReduceNextSpellCost(DynamicAmounts.fixed(1))
        description = "Whenever a Samurai or Warrior you control attacks alone, the next spell you " +
            "cast this turn costs {1} less to cast."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Micah Epstein"
        flavorText = "\"I fear I have run out of worthy opponents.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5b2176d-8925-4474-9d3e-1c97192715fb.jpg?1783923862"
    }
}
