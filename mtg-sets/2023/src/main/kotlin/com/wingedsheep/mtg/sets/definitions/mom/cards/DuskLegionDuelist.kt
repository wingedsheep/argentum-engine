package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dusk Legion Duelist — March of the Machine #11
 * {1}{W} · Creature — Vampire Soldier · 2/2
 *
 * Vigilance
 * Whenever one or more +1/+1 counters are put on this creature, draw a card. This ability
 * triggers only once each turn.
 *
 * Authored to Assay's compiled reading: a SELF-bound `CountersPlacedEvent` (+1/+1) with
 * `oncePerTurn = true` for "triggers only once each turn" — the restriction lives on the ability,
 * so the first counter batch each turn draws and later batches that turn don't.
 */
val DuskLegionDuelist = card("Dusk Legion Duelist") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Soldier"
    power = 2
    toughness = 2
    oracleText = "Vigilance\n" +
        "Whenever one or more +1/+1 counters are put on this creature, draw a card. This ability triggers only once each turn."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        effect = Effects.DrawCards(1)
        oncePerTurn = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Ryan Valle"
        flavorText = "\"We sailed to Ixalan guided by a promise—the Age of Everflowing Blood. No machine will take that from us.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/3/7305a8d3-5403-4483-92af-863dc91c6084.jpg?1783917064"
    }
}
