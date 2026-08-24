package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Tidehollow Strix
 * {U}{B}
 * Artifact Creature — Bird
 * 2 / 1
 *
 * Flying
 * Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)
 *
 * Two evergreen keywords and nothing else: [Keyword.FLYING] and [Keyword.DEATHTOUCH] go on the
 * card's keyword set, from which the builder derives one `Simple` keyword ability each. The
 * reminder text is part of the printed oracle text, not a separate ability.
 */
val TidehollowStrix = card("Tidehollow Strix") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Artifact Creature — Bird"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "Deathtouch (Any amount of damage this deals to a creature is enough to destroy it.)"

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Cyril Van Der Haegen"
        flavorText = "The scullers beneath Esper keep strixes as trained pets, and set them loose when a fare refuses to pay."
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4aafc65d-faa8-4bfe-83ac-7714c969022b.jpg"
    }
}
