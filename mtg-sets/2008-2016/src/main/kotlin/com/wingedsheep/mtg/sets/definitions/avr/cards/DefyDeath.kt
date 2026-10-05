package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Defy Death — Avacyn Restored #16
 * {3}{W}{W} · Sorcery
 *
 * Return target creature card from your graveyard to the battlefield. If it's an Angel, put two
 * +1/+1 counters on it.
 *
 * The Angel check runs after the return (an [Effects.If] over [Conditions.TargetMatchesFilter] on
 * the same target handle, which follows the card onto the battlefield — the Essence Flux shape), so
 * it reads the permanent's projected subtypes.
 *
 * Canonical printing is Avacyn Restored (its earliest real printing); Jumpstart 2022 is a
 * [com.wingedsheep.sdk.model.Printing] row (`.../definitions/j22/cards/DefyDeathReprint.kt`).
 */
val DefyDeath = card("Defy Death") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return target creature card from your graveyard to the battlefield. If it's an Angel, " +
        "put two +1/+1 counters on it."

    spell {
        val creature = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.PutOntoBattlefieldFromGraveyard(creature) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Creature.withSubtype(Subtype.ANGEL), creature),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, creature),
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Karl Kopinski"
        flavorText = "Gisela cornered the murderous demon among the graves of the dead villagers. " +
            "\"A fitting end,\" she murmured as she raised her sword."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/028028d7-80ff-4d63-8b84-795f257a3456.jpg?1783940738"
    }
}
