package com.wingedsheep.mtg.sets.definitions.c14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Containment Priest — Commander 2014 #5 (canonical printing)
 * {1}{W} · Creature — Human Cleric · 2/2
 * Flash
 * If a nontoken creature would enter and it wasn't cast, exile it instead.
 *
 * The [RedirectZoneChange] replacement on a battlefield entry, filtered to nontoken creatures that
 * weren't cast (`notCast()`): a spell resolving off the stack carries its cast origin, anything
 * reanimated, flickered, put onto the battlefield or played as a land (Dryad Arbor) doesn't. A
 * face-down entry (manifest) counts as a creature entering (ruling: judged as it would exist on the
 * battlefield). The Priest isn't on the battlefield while it enters itself, so it never exiles
 * itself or creatures entering alongside it.
 */
val ContainmentPriest = card("Containment Priest") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 2
    oracleText = "Flash\nIf a nontoken creature would enter and it wasn't cast, exile it instead."

    keywords(Keyword.FLASH)

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.nontoken().notCast(),
                to = Zone.BATTLEFIELD,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "John Stanko"
        flavorText = "Some protection requires a bit of finesse."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2c794b9-09da-49be-b258-b0e21f1663e3.jpg?1783938874"
    }
}
