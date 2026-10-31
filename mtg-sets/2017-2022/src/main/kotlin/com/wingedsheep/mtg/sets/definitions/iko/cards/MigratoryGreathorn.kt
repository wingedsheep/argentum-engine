package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Migratory Greathorn
 * {3}{G}
 * Creature — Beast
 * 3/4
 * Mutate {2}{G}
 * Whenever this creature mutates, search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle.
 */
val MigratoryGreathorn = card("Migratory Greathorn") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 4
    oracleText = "Mutate {2}{G} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Whenever this creature mutates, search your library for a basic land card, put it onto the battlefield tapped, then shuffle."

    keywordAbility(KeywordAbility.mutate("{2}{G}"))

    triggeredAbility {
        trigger = Triggers.self.mutates()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "165"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6a2a287b-b83f-444f-84f7-e388beb616c2.jpg?1783931033"
    }
}
