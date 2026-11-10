package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Oblivion Strike
 * {3}{B}
 * Sorcery
 * Devoid (This card has no color.)
 * Exile target creature.
 */
val OblivionStrike = card("Oblivion Strike") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Devoid (This card has no color.)\n" +
        "Exile target creature."

    keywords(Keyword.DEVOID)

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Exile(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Daniel Ljunggren"
        flavorText = "It's bad enough to be torn to pieces. It's worse by far to be torn from the world itself."
        imageUri = "https://cards.scryfall.io/normal/front/2/4/24e30acf-b30f-4bb3-b80e-dc7e9559da51.jpg"
    }
}
