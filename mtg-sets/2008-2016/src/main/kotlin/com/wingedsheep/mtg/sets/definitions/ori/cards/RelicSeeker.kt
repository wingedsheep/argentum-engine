package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Relic Seeker
 * {1}{W}
 * Creature — Human Soldier
 * 2/2
 *
 * Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)
 * When this creature becomes renowned, you may search your library for an Equipment card, reveal it, put it into your hand, then shuffle.
 *
 * The "may" gates the whole search, so declining also skips the shuffle (Thalia's Lancers shape).
 */
val RelicSeeker = card("Relic Seeker") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    oracleText = "Renown 1 (When this creature deals combat damage to a player, if it isn't renowned, put a +1/+1 counter on it and it becomes renowned.)\n" +
        "When this creature becomes renowned, you may search your library for an Equipment card, reveal it, put it into your hand, then shuffle."
    power = 2
    toughness = 2

    keywordAbility(KeywordAbility.renown(1))

    triggeredAbility {
        trigger = Triggers.self.becomesRenowned()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Artifact.withSubtype("Equipment"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true,
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/60a4b00c-5bd5-4931-be05-121f09a3fae4.jpg?1783938359"
        ruling("2015-06-22", "If a renown ability triggers, but the creature leaves the battlefield before that ability resolves, the creature doesn't become renowned. Any ability that triggers \"whenever a creature becomes renowned\" won't trigger.")
    }
}
