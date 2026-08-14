package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Frogmite — Mirrodin #172
 * {4} · Artifact Creature — Frog · 2/2
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 *
 * The stock [KeywordAbility.Affinity] cost reduction over [CardType.ARTIFACT]. Frogmite's cost is
 * entirely generic, so four artifacts make it free — affinity only shaves generic mana, and here
 * there is nothing else to shave.
 *
 * Affinity is a cost reduction, not an alternative cost: Frogmite's mana value stays 4 in every
 * zone no matter how cheaply it was cast, and the count is taken as the total cost is locked in
 * while casting.
 */
val Frogmite = card("Frogmite") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Frog"
    power = 2
    toughness = 2
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)"

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "172"
        artist = "Terese Nielsen"
        flavorText = "At first, vedalken observers thought blinkmoths naturally avoided certain " +
            "places. Then they realized those places were frogmite feeding grounds."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff504dcb-2eb8-4b3c-a8b9-29697739b649.jpg?1783944522"
    }
}
