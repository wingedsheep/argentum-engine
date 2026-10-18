package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Prosperous Thief — Kamigawa: Neon Dynasty #73 (canonical printing)
 * {2}{U} · Creature — Human Ninja · 3/2
 *
 * Ninjutsu {1}{U}
 * Whenever one or more Ninja or Rogue creatures you control deal combat damage to a player, create a
 * Treasure token.
 *
 * The Treasure is a batch trigger (CR 603.2c): it fires once per combat damage step per damaged
 * player, however many Ninjas and Rogues connected. "You control" is the event's own.
 */
val ProsperousThief = card("Prosperous Thief") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Ninja"
    power = 3
    toughness = 2
    oracleText = "Ninjutsu {1}{U} ({1}{U}, Return an unblocked attacker you control to hand: Put this " +
        "card onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever one or more Ninja or Rogue creatures you control deal combat damage to a player, " +
        "create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of " +
        "any color.\")"

    ninjutsu("{1}{U}")

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature.withAnySubtype("Ninja", "Rogue"))
            .dealCombatDamageToAPlayer()
        effect = Effects.CreateTreasure()
        description = "Whenever one or more Ninja or Rogue creatures you control deal combat damage to " +
            "a player, create a Treasure token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "73"
        artist = "Fajareka Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da3f683e-5a28-4ae7-b53b-851b957123ba.jpg?1783923897"
    }
}
