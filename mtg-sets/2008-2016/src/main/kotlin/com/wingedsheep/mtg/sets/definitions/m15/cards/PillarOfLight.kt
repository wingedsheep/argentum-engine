package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Pillar of Light
 * {2}{W}
 * Instant
 * Exile target creature with toughness 4 or greater.
 */
val PillarOfLight = card("Pillar of Light") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Exile target creature with toughness 4 or greater."

    spell {
        val creature = target(TargetFilter.Creature.toughnessAtLeast(4))
        effect = Effects.Exile(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "24"
        artist = "Erica Yang"
        flavorText = "\"The vaulted ceiling of our faith rests upon such pillars.\"\n—Darugand, banisher priest"
        imageUri = "https://cards.scryfall.io/normal/front/0/1/018ff99e-a23d-4440-8c5c-cfb4942f1906.jpg"
    }
}
