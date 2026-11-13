package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Nissa's Renewal
 * {5}{G}
 * Sorcery
 * Search your library for up to three basic land cards, put them onto the battlefield tapped,
 * then shuffle. You gain 7 life.
 */
val NissasRenewal = card("Nissa's Renewal") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for up to three basic land cards, put them onto the battlefield tapped, " +
        "then shuffle. You gain 7 life."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            count = 3,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
            shuffleAfter = true
        ) then
            Effects.GainLife(7)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "180"
        artist = "Lius Lasahido"
        flavorText = "Her connection to the plane restored, Nissa felt the power of Zendikar surge through her."
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91aa2ff1-12ea-4896-a955-a9cd76c3b49d.jpg?1783938187"
    }
}
