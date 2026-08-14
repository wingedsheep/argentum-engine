package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Lumbering Worldwagon
 * {2}{G}
 * Artifact — Vehicle
 * * /4
 * This Vehicle's power is equal to the number of lands you control.
 * Whenever this Vehicle enters or attacks, you may search your library for a basic land card,
 * put it onto the battlefield tapped, then shuffle.
 * Crew 4
 *
 * Only power is dynamic (printed toughness stays 4), so the single-stat `dynamicPower(...)`
 * helper applies the Layer 7b characteristic-defining ability the way Kraven, Proud Predator
 * does — `DynamicAmounts.landsYouControl()` is the same aggregate Molimo uses for both stats.
 *
 * "Enters or attacks" is the repo's established two-ability idiom (Sentinel of the Nameless
 * City, Queen's Bay Paladin, Visage of Dread): there is no single enters-or-attacks
 * `TriggerSpec`, so [Triggers.EntersBattlefield] and [Triggers.Attacks] share one
 * [MayEffect]-wrapped search. The "you may" is a decline of the whole search (Quirion
 * Trailblazer), not a failure-to-find, so it wraps the pattern rather than living inside it.
 */
val LumberingWorldwagon = card("Lumbering Worldwagon") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Vehicle"
    toughness = 4
    oracleText = "This Vehicle's power is equal to the number of lands you control.\n" +
        "Whenever this Vehicle enters or attacks, you may search your library for a basic land card, " +
        "put it onto the battlefield tapped, then shuffle.\n" +
        "Crew 4"

    dynamicPower(DynamicAmounts.landsYouControl())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = MayEffect(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
    }

    keywordAbility(KeywordAbility.crew(4))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "168"
        artist = "Raph Lomotan"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f989c59-7b2a-4036-9ea2-cd0c7e85c15b.jpg?1783907870"
    }
}
