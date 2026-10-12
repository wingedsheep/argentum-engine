package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unearth
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Simian Simulacrum
 * {3}
 * Artifact Creature — Ape
 * 2/1
 * When this creature enters, put two +1/+1 counters on target creature you control.
 * Unearth {2}{G}{G}
 *
 * The target may be the Simulacrum itself — it is on the battlefield when the trigger goes on the stack.
 */
val SimianSimulacrum = card("Simian Simulacrum") {
    manaCost = "{3}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Ape"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, put two +1/+1 counters on target creature you control.\n" +
        "Unearth {2}{G}{G} ({2}{G}{G}: Return this card from your graveyard to the battlefield. It gains haste. Exile it " +
        "at the beginning of the next end step or if it would leave the battlefield. Unearth only as a sorcery.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature)
    }

    unearth("{2}{G}{G}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "205"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c85e2a35-cb55-434c-bbd7-54c3438345c1.jpg?1783920033"
    }
}
