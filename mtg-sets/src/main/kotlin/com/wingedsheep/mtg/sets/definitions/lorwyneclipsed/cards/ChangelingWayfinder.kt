package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Changeling Wayfinder
 * {3}
 * Creature — Shapeshifter
 * 1/2
 *
 * Changeling (This card is every creature type.)
 * When this creature enters, you may search your library for a basic land card,
 * reveal it, put it into your hand, then shuffle.
 */
val ChangelingWayfinder = card("Changeling Wayfinder") {
    manaCost = "{3}"
    typeLine = "Creature — Shapeshifter"
    power = 1
    toughness = 2
    oracleText = "Changeling (This card is every creature type.)\n" +
        "When this creature enters, you may search your library for a basic land card, reveal it, put it into your hand, then shuffle."

    keywords(Keyword.CHANGELING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            EffectPatterns.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "1"
        artist = "Quintin Gleim"
        flavorText = "No map. No complaints."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c6061aa-a4da-4115-85b6-d0aa22f2386c.jpg?1767956913"
    }
}
