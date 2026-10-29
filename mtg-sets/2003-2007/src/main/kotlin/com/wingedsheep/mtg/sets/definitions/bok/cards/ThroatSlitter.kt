package com.wingedsheep.mtg.sets.definitions.bok.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.ninjutsu
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Throat Slitter — Betrayers of Kamigawa #88 (canonical printing)
 * {4}{B} · Creature — Rat Ninja · 2/2
 *
 * Ninjutsu {2}{B}
 * Whenever this creature deals combat damage to a player, destroy target nonblack creature that
 * player controls.
 *
 * "That player" is the damaged player — `controlledByTriggeringPlayer()`, as on Mistblade Shinobi.
 */
val ThroatSlitter = card("Throat Slitter") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat Ninja"
    power = 2
    toughness = 2
    oracleText = "Ninjutsu {2}{B} ({2}{B}, Return an unblocked attacker you control to hand: Put this " +
        "card onto the battlefield from your hand tapped and attacking.)\n" +
        "Whenever this creature deals combat damage to a player, destroy target nonblack creature " +
        "that player controls."

    ninjutsu("{2}{B}")

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(TargetFilter.Creature.notColor(Color.BLACK).controlledByTriggeringPlayer())
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4dcfdb4-c2ff-4fc5-aa2a-0c6e311d2910.jpg?1783944194"
    }
}
