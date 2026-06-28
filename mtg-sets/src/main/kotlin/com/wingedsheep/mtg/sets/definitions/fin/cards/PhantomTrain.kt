package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phantom Train — Final Fantasy #110
 * {3}{B} · Artifact — Vehicle · 4/4 · Uncommon
 *
 * Trample
 * Sacrifice another artifact or creature: Put a +1/+1 counter on this Vehicle.
 * It becomes a Spirit artifact creature in addition to its other types until end of turn.
 *
 * The activated ability self-animates the Vehicle (Vehicles aren't creatures until something
 * makes them one). The cost is [Costs.SacrificeAnother] of an artifact-or-creature (excludes the
 * Train itself). The effect is a [Effects.Composite] of:
 *   1. [Effects.AddCounters] — a permanent +1/+1 counter on the source (the counter persists past
 *      end of turn even though the animation does not, matching the printed wording).
 *   2. [Effects.BecomeCreature] on the source with base 4/4 (its printed Vehicle stats), adding the
 *      Spirit creature subtype and the ARTIFACT card type, for `Duration.EndOfTurn`. BecomeCreature
 *      always adds CREATURE without removing existing types, so the permanent stays a Vehicle —
 *      "in addition to its other types". The +1/+1 counter then layers on top, so while animated it
 *      is a 5/5 (and 4/4 again, but uncountered, after the turn ends until it is animated again).
 */
val PhantomTrain = card("Phantom Train") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Vehicle"
    oracleText = "Trample\n" +
        "Sacrifice another artifact or creature: Put a +1/+1 counter on this Vehicle. " +
        "It becomes a Spirit artifact creature in addition to its other types until end of turn."
    power = 4
    toughness = 4
    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.CreatureOrArtifact)
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 4,
                toughness = 4,
                creatureTypes = setOf("Spirit"),
                addTypes = setOf("ARTIFACT"),
                duration = Duration.EndOfTurn
            )
        )
        description = "Sacrifice another artifact or creature: Put a +1/+1 counter on this Vehicle. " +
            "It becomes a Spirit artifact creature in addition to its other types until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Gal Or"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a50d2ac-101d-41e1-b400-18fa7d2d7125.jpg?1748706172"
    }
}
