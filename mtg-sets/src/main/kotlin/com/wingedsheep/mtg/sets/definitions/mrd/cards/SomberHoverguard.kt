package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Somber Hoverguard — Mirrodin #51
 * {5}{U} · Creature — Drone · 3/2
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * Flying
 *
 * The blue affinity common. Unlike [Frogmite], only the {5} is shaveable — affinity reduces
 * generic mana only, so the {U} always has to be paid however many artifacts are out, and the
 * floor is one blue mana rather than free.
 *
 * Affinity is a cost reduction rather than an alternative cost: the mana value stays 6 in every
 * zone no matter how cheaply it was cast.
 */
val SomberHoverguard = card("Somber Hoverguard") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drone"
    power = 3
    toughness = 2
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "Flying"

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))
    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "51"
        artist = "Adam Rex"
        flavorText = "The vedalken interrogate all intruders—once the hoverguards are done with them."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83a46ac2-d96d-4d5a-94fc-a9fc83a9ea1d.jpg?1783944550"
    }
}
