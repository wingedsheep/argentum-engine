package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Quirion Trailblazer
 * {3}{G}
 * Creature — Elf Scout
 * 1/2
 * When this creature enters, you may search your library for a basic land card,
 * put that card onto the battlefield tapped, then shuffle.
 */
val QuirionTrailblazer = card("Quirion Trailblazer") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, you may search your library for a basic land card, " +
        "put that card onto the battlefield tapped, then shuffle."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            LibraryPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "205"
        artist = "Rebecca Guay"
        flavorText = "\"All that matters is the path ahead.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2b258c1-5fb4-4072-bb32-ad364df1874a.jpg?1562934099"
    }
}
