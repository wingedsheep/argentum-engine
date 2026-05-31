package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Embermouth Sentinel
 * {2}
 * Artifact Creature — Chimera
 * 2/1
 *
 * When this creature enters, you may search your library for a basic land card, reveal it,
 * then shuffle and put that card on top. If you control a Dragon, put that card onto the
 * battlefield tapped instead.
 *
 * Modeled as a [ConditionalEffect] on "you control a Dragon", checked at resolution. The
 * search is a single optional library search (ChooseUpTo via [EffectPatterns.searchLibrary]);
 * only one destination branch runs:
 *  - Control a Dragon → put the basic land onto the battlefield tapped, then shuffle.
 *  - Otherwise → shuffle, then put the basic land on top of the library.
 * Both branches reveal the found card per the oracle text.
 */
val EmbermouthSentinel = card("Embermouth Sentinel") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Chimera"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, you may search your library for a basic land card, " +
        "reveal it, then shuffle and put that card on top. If you control a Dragon, put that " +
        "card onto the battlefield tapped instead."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ConditionalEffect(
            condition = Conditions.ControlCreatureOfType(Subtype.DRAGON),
            // Control a Dragon: put the basic land onto the battlefield tapped instead.
            effect = EffectPatterns.searchLibrary(
                filter = Filters.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
                shuffleAfter = true,
                reveal = true
            ),
            // Otherwise: shuffle and put the basic land on top of the library.
            elseEffect = EffectPatterns.searchLibrary(
                filter = Filters.BasicLand,
                count = 1,
                destination = SearchDestination.TOP_OF_LIBRARY,
                shuffleAfter = true,
                reveal = true
            )
        )
        description = "you may search your library for a basic land card, reveal it, then shuffle and " +
            "put that card on top. If you control a Dragon, put that card onto the battlefield tapped instead."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "242"
        artist = "Stephanie Cheung"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/485f75d5-da5b-4605-885a-561ccd999cc6.jpg?1743204957"
    }
}
