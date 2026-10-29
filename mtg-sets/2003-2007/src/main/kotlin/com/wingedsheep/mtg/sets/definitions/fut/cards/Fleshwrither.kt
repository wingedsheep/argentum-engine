package com.wingedsheep.mtg.sets.definitions.fut.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Fleshwrither
 * {2}{B}{B}
 * Creature — Horror
 * 3/3
 *
 * Transfigure {1}{B}{B} ({1}{B}{B}, Sacrifice this creature: Search your library for a creature
 * card with the same mana value as this creature, put that card onto the battlefield, then
 * shuffle. Transfigure only as a sorcery.)
 *
 * Transfigure is written out as the plain sorcery-speed activated ability its reminder text
 * spells. The mana value is read from the sacrificed creature's cost-payment snapshot
 * ([DynamicAmounts.sacrificedManaValue]), not fixed from the printed cost, so an object that
 * gained this ability (Necrotic Ooze) or a copy searches for its own mana value.
 */
val Fleshwrither = card("Fleshwrither") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horror"
    power = 3
    toughness = 3
    oracleText = "Transfigure {1}{B}{B} ({1}{B}{B}, Sacrifice this creature: Search your library for " +
        "a creature card with the same mana value as this creature, put that card onto the " +
        "battlefield, then shuffle. Transfigure only as a sorcery.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}{B}"), Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.manaValueEqualsDynamic(DynamicAmounts.sacrificedManaValue()),
            destination = SearchDestination.BATTLEFIELD
        )
        description = "Transfigure {1}{B}{B} — Sacrifice Fleshwrither to search for a creature card " +
            "with the same mana value and put it onto the battlefield."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Dave Allsop"
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08e1a646-6925-4a53-a99c-8951af016a7a.jpg?1783943112"
        ruling("2007-05-01", "Transfigure is similar to Transmute, though it can be activated only if the permanent with Transfigure is on the battlefield, and it can fetch only a creature.")
    }
}
