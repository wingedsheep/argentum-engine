package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.webSlinging
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Spider-Man, Brooklyn Visionary — Marvel's Spider-Man #115
 * {4}{G} · Legendary Creature — Spider Human Hero · 4/3
 *
 * Web-slinging {2}{G}
 * When Spider-Man enters, search your library for a basic land card, put it onto the battlefield
 * tapped, then shuffle.
 */
val SpiderManBrooklynVisionary = card("Spider-Man, Brooklyn Visionary") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Spider Human Hero"
    power = 4
    toughness = 3
    oracleText = "Web-slinging {2}{G} (You may cast this spell for {2}{G} if you also return a " +
        "tapped creature you control to its owner's hand.)\n" +
        "When Spider-Man enters, search your library for a basic land card, put it onto the " +
        "battlefield tapped, then shuffle."

    webSlinging("{2}{G}")

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
        description = "When Spider-Man enters, search your library for a basic land card, put it " +
            "onto the battlefield tapped, then shuffle."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "115"
        artist = "Aniekan Udofia"
        flavorText = "\"Nothing like a little crimefighting after class.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e19929bc-cbe1-4970-952d-8e9d0193ddce.jpg?1783905323"
    }
}
