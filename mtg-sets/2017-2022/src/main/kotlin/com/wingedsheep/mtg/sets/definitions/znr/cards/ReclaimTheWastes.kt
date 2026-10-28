package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Reclaim the Wastes — Zendikar Rising #200
 * {G} · Sorcery
 *
 * Kicker {3}
 * Search your library for a basic land card, reveal it, put it into your hand, then shuffle.
 * If this spell was kicked, search your library for two basic land cards instead of one.
 */
val ReclaimTheWastes = card("Reclaim the Wastes") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Kicker {3} (You may pay an additional {3} as you cast this spell.)\n" +
        "Search your library for a basic land card, reveal it, put it into your hand, then shuffle. " +
        "If this spell was kicked, search your library for two basic land cards instead of one."

    keywordAbility(KeywordAbility.kicker("{3}"))

    spell {
        effect = Effects.If(
            condition = WasKicked,
            then = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 2,
                destination = SearchDestination.HAND,
                shuffleAfter = true,
                reveal = true
            ),
            otherwise = Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.HAND,
                shuffleAfter = true,
                reveal = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "200"
        artist = "Wylie Beckert"
        imageUri = "https://cards.scryfall.io/normal/front/8/0/805faf2e-97dc-4b77-bc02-a4cae980f041.jpg?1783929334"
    }
}
