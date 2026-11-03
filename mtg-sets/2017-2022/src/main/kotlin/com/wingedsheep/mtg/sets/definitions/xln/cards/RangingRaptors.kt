package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ranging Raptors
 * {2}{G}
 * Creature — Dinosaur
 * 2/3
 * Enrage — Whenever this creature is dealt damage, you may search your library for a basic land
 * card, put it onto the battlefield tapped, then shuffle.
 *
 * "Enrage" is an ability word (CR 207.2c), so it lives in the trigger's description.
 */
val RangingRaptors = card("Ranging Raptors") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dinosaur"
    power = 2
    toughness = 3
    oracleText = "Enrage — Whenever this creature is dealt damage, you may search your library for a basic land card, put it onto the battlefield tapped, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = Filters.BasicLand,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
        description = "Enrage — Whenever this creature is dealt damage, you may search your library for a basic land card, put it onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Simon Dominic"
        flavorText = "They cover their territory like a tide of teeth and claws."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e91efc6-0e6a-4a9e-a486-adf53e53d3f1.jpg?1783935720"
    }
}
