package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.firebending
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter

/**
 * Cruel Administrator
 * {3}{B}{R}
 * Creature — Human Soldier
 * 5/4
 *
 * Raid — This creature enters with a +1/+1 counter on it if you attacked this turn.
 * Whenever this creature attacks, create a 2/2 red Soldier creature token with firebending 1.
 * (Whenever it attacks, add {R}. This mana lasts until end of combat.)
 *
 * Raid (the conditional enter-with-counter) is a self-only [EntersWithCounters] replacement effect
 * gated on [Conditions.YouAttackedThisTurn] — the counter is added as the creature enters iff you
 * attacked this turn (CR 614, evaluated at the moment of entry). The attack trigger creates the
 * Soldier token; the token itself carries firebending 1, modeled via the [firebending] DSL on the
 * inline token card definition ([firebendingSoldierToken]). Firebending is a display-only keyword
 * backed by an attack-triggered "add {R} (until end of combat)" ability, so the token gets the
 * [Keyword.FIREBENDING] keyword plus that same triggered ability (sourced from the token def).
 */
private val firebendingSoldierToken = card("Soldier") {
    typeLine = "Token Creature — Soldier"
    colorIdentity = "R"
    power = 2
    toughness = 2
    firebending(1)
}

val CruelAdministrator = card("Cruel Administrator") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Soldier"
    power = 5
    toughness = 4
    oracleText = "Raid — This creature enters with a +1/+1 counter on it if you attacked this turn.\n" +
        "Whenever this creature attacks, create a 2/2 red Soldier creature token with firebending 1. " +
        "(Whenever it attacks, add {R}. This mana lasts until end of combat.)"

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 1,
            selfOnly = true,
            condition = Conditions.YouAttackedThisTurn
        )
    )

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = CreateTokenEffect(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Soldier"),
            keywords = setOf(Keyword.FIREBENDING),
            triggeredAbilities = firebendingSoldierToken.triggeredAbilities,
            imageUri = "https://cards.scryfall.io/normal/front/2/d/2de43b03-9ac5-4292-ab29-2dc6210ef3d9.jpg?1777982247"
        )
        description = "Whenever this creature attacks, create a 2/2 red Soldier creature token with firebending 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "213"
        artist = "Norikatsu Miyoshi"
        flavorText = "\"You're one mistake away from dying where you stand.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d70707b-2e25-40b1-80a8-9a5492bca7e2.jpg?1764121522"
    }
}
