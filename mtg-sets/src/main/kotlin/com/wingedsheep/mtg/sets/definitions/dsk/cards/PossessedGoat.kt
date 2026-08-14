package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Possessed Goat
 * {W}
 * Creature — Goat
 * {3}, Discard a card: Put three +1/+1 counters on this creature and it becomes a black Demon
 * in addition to its other colors and types. Activate only once.
 * 1/1
 *
 * "becomes a black Demon in addition to its other colors and types" is modeled additively:
 * [Effects.AddColor] (Layer 5, keeps White) + [Effects.AddCreatureType] (Layer 4, keeps Goat),
 * both Permanent. "Activate only once" maps to [ActivationRestriction.Once] (once for the
 * lifetime of this permanent).
 */
val PossessedGoat = card("Possessed Goat") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Goat"
    power = 1
    toughness = 1
    oracleText = "{3}, Discard a card: Put three +1/+1 counters on this creature and it becomes a " +
        "black Demon in addition to its other colors and types. Activate only once."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.DiscardCard)
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self),
            Effects.AddColor(Color.BLACK, EffectTarget.Self, Duration.Permanent),
            Effects.AddCreatureType("Demon", EffectTarget.Self, Duration.Permanent),
        )
        restrictions = listOf(ActivationRestriction.Once)
        description = "{3}, Discard a card: Put three +1/+1 counters on this creature and it " +
            "becomes a black Demon in addition to its other colors and types. Activate only once."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "25"
        artist = "Edgar Sánchez Hidalgo"
        flavorText = "When their would-be sacrifice wandered back to their encampment, the survivors " +
            "breathed a sigh of relief, assuming the demon had departed."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd02b8ff-ff65-4a38-b8fb-f8dd130edbf7.jpg?1726285949"
        imageUriByCreatureSubtype = mapOf("Demon" to "/images/custom/possessed-goat.jpeg")
    }
}
