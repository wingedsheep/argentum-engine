package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

val ShieldWallSentinel = card("Shield-Wall Sentinel") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Golem"
    power = 1
    toughness = 3
    oracleText = "Defender\n" +
        "When this creature enters, you may search your library for a creature card with defender, " +
        "reveal it, put it into your hand, then shuffle."

    keywords(Keyword.DEFENDER)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Creature.withKeyword(Keyword.DEFENDER),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true,
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "238"
        artist = "Titus Lunter"
        flavorText = "\"Unit alpha-6 locking into position!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e57e1d7-da1d-4a2b-867f-85b8331b7bbc.jpg?1783921266"
    }
}
