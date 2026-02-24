package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.Effects

/**
 * Screaming Seahawk
 * {4}{U}
 * Creature — Bird
 * 2/2
 * Flying
 * When Screaming Seahawk enters the battlefield, you may search your library for a card
 * named Screaming Seahawk, reveal it, put it into your hand, then shuffle.
 */
val ScreamingSeahawk = card("Screaming Seahawk") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 2
    oracleText = "Flying\nWhen Screaming Seahawk enters the battlefield, you may search your library for a card named Screaming Seahawk, reveal it, put it into your hand, then shuffle."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Effects.SearchLibrary(
                filter = GameObjectFilter.Any.named("Screaming Seahawk"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffle = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc5856ac-e710-44ee-8516-6070f4f31ce5.jpg?1562943230"
    }
}
