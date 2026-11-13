package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Grip of Desolation
 * {4}{B}{B}
 * Instant
 * Devoid (This card has no color.)
 * Exile target creature and target land.
 *
 * Two independent single-count target requirements, each read through its own handle (Rise of
 * Extus's shape). Both are required, so the spell can't be cast without a legal creature and a legal
 * land. Separate instances of "target" may pick the same object, so a land creature can fill both
 * slots; the second exile is then a no-op. If one target becomes illegal, the other is still exiled.
 */
val GripOfDesolation = card("Grip of Desolation") {
    manaCost = "{4}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Devoid (This card has no color.)\n" +
        "Exile target creature and target land."

    keywords(Keyword.DEVOID)

    spell {
        val creature = target(TargetFilter.Creature)
        val land = target(TargetFilter.Land)
        effect = Effects.Exile(creature) then Effects.Exile(land)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Lius Lasahido"
        flavorText = "As the hand closed, there was first terror, then numbness, and finally nothingness."
        imageUri = "https://cards.scryfall.io/normal/front/8/5/850b388a-c188-433a-8124-1d51b43a4e51.jpg?1783938205"
        ruling("2015-08-25", "You must target a creature and a land in order to cast Grip of Desolation. A permanent that's both a land and a creature can be chosen for both targets.")
    }
}
