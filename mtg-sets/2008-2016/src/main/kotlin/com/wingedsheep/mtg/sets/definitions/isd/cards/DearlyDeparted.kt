package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dearly Departed
 * {4}{W}{W}
 * Creature — Spirit
 * 5/5
 * Flying
 * As long as this creature is in your graveyard, each Human creature you control enters with an
 * additional +1/+1 counter on it.
 *
 * [com.wingedsheep.mtg.sets.definitions.blc.cards.GrumgullyTheGenerous]'s
 * [EntersWithDynamicCounters] with its zone moved: `activeZones = setOf(Zone.GRAVEYARD)` is the
 * whole of "as long as this creature is in your graveyard" (CR 113.6). The scanner reads that
 * field, so the same declaration both switches the grant **on** from the graveyard and **off** on
 * the battlefield — a Dearly Departed you cast is a plain 5/5 flier until it dies.
 *
 * `otherOnly = true` is what routes the effect through the global sweep rather than the source's
 * own entry path; it is not a narrowing here, since the Spirit is never one of the Humans it
 * pumps, and it cannot be live for its own entry anyway (entering the battlefield is leaving the
 * graveyard).
 *
 * "Each Human creature you control" resolves `youControl` against the *owner* of the graveyard,
 * which is the only sensible reading of "you" for a card outside the battlefield (CR 108.3) — and
 * the printed ruling that the effect is cumulative falls out of the sweep visiting every card in
 * the graveyard: two copies hand out two counters.
 */
val DearlyDeparted = card("Dearly Departed") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    oracleText = "Flying\n" +
        "As long as this creature is in your graveyard, each Human creature you control enters " +
        "with an additional +1/+1 counter on it."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithDynamicCounters(
            count = DynamicAmount.Fixed(1),
            otherOnly = true,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().withSubtype(Subtype.HUMAN),
                to = Zone.BATTLEFIELD,
            ),
            activeZones = setOf(Zone.GRAVEYARD),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Daniel Ljunggren"
        flavorText = "\"Never forget our ancestors. They have not forgotten us.\"\n—Mikaeus, the Lunarch"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d008061f-cda4-4bcf-b6b3-d1b4a251cc66.jpg?1783940996"
        ruling("2011-09-22", "The effect is cumulative. Human creatures you control will enter with a +1/+1 counter for each Dearly Departed in your graveyard.")
        ruling("2011-09-22", "In most cases, when determining whether a creature entering under your control should get a +1/+1 counter, you'll simply look at what the creature will look like on the battlefield. You'll consider any effects affecting a creature entering under your control.")
    }
}
