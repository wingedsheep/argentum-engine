package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.readAhead
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * The Weatherseed Treaty
 * {2}{G}
 * Enchantment — Saga
 *
 * Read ahead
 * I — Search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 * II — Create a 1/1 green Saproling creature token.
 * III — Domain — Target creature you control gets +X/+X and gains trample until end of turn,
 *       where X is the number of basic land types among lands you control.
 */
val TheWeatherseedTreaty = card("The Weatherseed Treaty") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "Read ahead (Choose a chapter and start with that many lore counters. Add one " +
        "after your draw step. Skipped chapters don't trigger. Sacrifice after III.)\n" +
        "I — Search your library for a basic land card, put it onto the battlefield tapped, then " +
        "shuffle.\n" +
        "II — Create a 1/1 green Saproling creature token.\n" +
        "III — Domain — Target creature you control gets +X/+X and gains trample until end of " +
        "turn, where X is the number of basic land types among lands you control."

    readAhead()

    sagaChapter(1) {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true,
        )
    }

    sagaChapter(2) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling"),
            imageUri = "https://cards.scryfall.io/normal/front/d/5/d5a07153-9eb6-4366-aa09-ebe9b4868e7f.jpg?1783921125",
        )
    }

    sagaChapter(3) {
        val creature = target(TargetFilter.CreatureYouControl)
        val domain = DynamicAmounts.domain()
        effect = Effects.ModifyStats(domain, domain, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2a54461-1f75-4304-b6d7-635171419456.jpg?1783921290"

        ruling("2022-09-09", "Domain abilities count the number of basic land types among lands you control, not how many lands you control or how many of any type.")
        ruling("2022-09-09", "As a Saga with read ahead enters the battlefield, its controller chooses a number from one to that Saga's greatest chapter number. The Saga enters the battlefield with the chosen number of lore counters. Neither choosing the number nor putting the counters on the Saga use the stack, and neither can be responded to.")
    }
}
