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
 * Daru Cavalier
 * {3}{W}
 * Creature — Human Soldier
 * 2/2
 * First strike
 * When Daru Cavalier enters the battlefield, you may search your library for a card
 * named Daru Cavalier, reveal it, put it into your hand, then shuffle.
 */
val DaruCavalier = card("Daru Cavalier") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "First strike\nWhen Daru Cavalier enters the battlefield, you may search your library for a card named Daru Cavalier, reveal it, put it into your hand, then shuffle."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = MayEffect(
            Effects.SearchLibrary(
                filter = GameObjectFilter.Any.named("Daru Cavalier"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffle = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Dany Orizio"
        imageUri = "https://cards.scryfall.io/large/front/e/b/eb2e9b7e-434e-477f-b3e8-e85ceb913650.jpg?1562951000"
    }
}
