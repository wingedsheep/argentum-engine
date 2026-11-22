package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Bogbrew Witch
 * {3}{B}
 * Creature — Human Wizard
 * 1/3
 * {2}, {T}: Search your library for a card named Festering Newt or Bubbling Cauldron,
 * put it onto the battlefield tapped, then shuffle.
 */
val BogbrewWitch = card("Bogbrew Witch") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "{2}, {T}: Search your library for a card named Festering Newt or Bubbling Cauldron, " +
        "put it onto the battlefield tapped, then shuffle."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.named("Festering Newt") or GameObjectFilter.Any.named("Bubbling Cauldron"),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "88"
        artist = "Eric Deschamps"
        flavorText = "\"The hardest thing is finding the right ingredients.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/5/7559cf3e-7fad-4bcf-8551-045f9150e014.jpg?1783939926"
    }
}
