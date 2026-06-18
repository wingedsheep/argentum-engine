package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * You Cannot Pass!
 * {W}
 * Instant
 *
 * Destroy target creature that blocked or was blocked by a legendary creature this turn.
 *
 * The target filter uses the new `StatePredicate.BlockedOrWasBlockedByLegendaryThisTurn`
 * (`blockedOrWasBlockedByLegendaryThisTurn()`), backed by a per-creature marker stamped at
 * block declaration. The marker captures the legendary partner's status at pairing time, so
 * the spell can still target the creature even after that legendary creature has left the
 * battlefield (per the card's ruling).
 */
val YouCannotPass = card("You Cannot Pass!") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target creature that blocked or was blocked by a legendary creature this turn."

    spell {
        val t = target(
            "target creature that blocked or was blocked by a legendary creature this turn",
            TargetCreature(
                filter = TargetFilter(GameObjectFilter.Creature.blockedOrWasBlockedByLegendaryThisTurn())
            )
        )
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Leonardo Borazio"
        flavorText = "It raised the whip, and the thongs whined and cracked. Fire came from its nostrils. But Gandalf stood firm."
        imageUri = "https://cards.scryfall.io/normal/front/1/1/116d4030-acd2-4aa2-8254-aaaff1264459.jpg?1686967987"
    }
}
