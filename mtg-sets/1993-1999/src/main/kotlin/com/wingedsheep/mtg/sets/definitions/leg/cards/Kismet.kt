package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped

/**
 * Kismet
 * {3}{W}
 * Enchantment
 *
 * Artifacts, creatures, and lands your opponents control enter tapped.
 *
 * Same shape as Loxodon Gatekeeper: three separate [PermanentsEnterTapped] replacements, one per
 * card type, since "artifact", "creature" and "land" are independent characteristic checks — an
 * artifact creature matches more than one and simply enters tapped once. The controller-relative
 * `opponentControls()` predicate resolves against Kismet's controller at entry time, so every
 * opponent is affected.
 */
val Kismet = card("Kismet") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Artifacts, creatures, and lands your opponents control enter tapped."

    // Artifacts your opponents control enter tapped.
    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Artifact.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    // Creatures your opponents control enter tapped.
    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    // Lands your opponents control enter tapped.
    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Land.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Kaja Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e0651ad-6901-4f9b-8807-d66e53a4ada8.jpg?1783948083"
        ruling(
            "2007-02-01",
            "The appropriate permanents enter tapped. They do not enter untapped and then " +
                "immediately tap, therefore they do not trigger any effects due to tapping."
        )
        ruling("2007-02-01", "It affects all opponents.")
    }
}
