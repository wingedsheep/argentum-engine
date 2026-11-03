package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary

/**
 * Captain Pike, Fullness of Time
 * {1}{G}{G}
 * Legendary Creature — Human Officer Scout
 * Star/star (characteristic-defining)
 *
 * Strange new worlds — Captain Pike's power and toughness are each equal to the number of
 * differently named lands you control.
 * You may look at the top card of your library any time.
 * You may play lands from the top of your library.
 *
 * "Strange new worlds" is an ability word with no rules meaning. The P/T is a
 * characteristic-defining [dynamicStats] over the TRK count; the top-of-library pair is
 * [LookAtTopOfLibrary] (private look) plus Courser of Kruphix's lands-only
 * [PlayLandsAndCastFilteredFromTopOfLibrary] with `spellFilter = null`.
 */
val CaptainPikeFullnessOfTime = card("Captain Pike, Fullness of Time") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Officer Scout"
    oracleText = "Strange new worlds — Captain Pike's power and toughness are each equal to the number of " +
        "differently named lands you control.\n" +
        "You may look at the top card of your library any time.\n" +
        "You may play lands from the top of your library."

    dynamicStats(DynamicAmounts.differentlyNamedLandsYouControl())

    staticAbility { ability = LookAtTopOfLibrary() }

    staticAbility {
        ability = PlayLandsAndCastFilteredFromTopOfLibrary(spellFilter = null)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "Joel Chaim Holtzman"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/743be4c4-5c91-42bb-9942-115e903041a9.jpg?1792508262"
    }
}
