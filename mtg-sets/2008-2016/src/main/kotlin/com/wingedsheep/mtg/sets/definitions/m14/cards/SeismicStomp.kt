package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Seismic Stomp
 * {1}{R}
 * Sorcery
 * Creatures without flying can't block this turn.
 *
 * The restriction is a dynamic group restriction (it doesn't change characteristics), so creatures
 * that lose flying or enter without flying later this turn can't block either.
 */
val SeismicStomp = card("Seismic Stomp") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Creatures without flying can't block this turn."

    spell {
        effect = Effects.CantBlockGroup(
            GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Chase Stone"
        flavorText = "\"It is not in me to tread lightly.\"\n—Ryda, geomancer"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f55a02a3-8b65-44a7-82ef-2d3dc05d00ab.jpg?1783939911"
        ruling(
            "2016-06-08",
            "No creature without flying will be able to block that turn, including creatures that lose flying after Seismic Stomp resolves and creatures without flying that enter the battlefield after Seismic Stomp resolves."
        )
    }
}
