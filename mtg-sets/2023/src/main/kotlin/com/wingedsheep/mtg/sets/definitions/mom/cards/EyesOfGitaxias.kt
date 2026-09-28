package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Eyes of Gitaxias
 * {2}{U}
 * Sorcery
 * Incubate 3. Draw a card.
 */
val EyesOfGitaxias = card("Eyes of Gitaxias") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Incubate 3. (Create an Incubator token with three +1/+1 counters on it and \"{2}: Transform this token.\" " +
        "It transforms into a 0/0 Phyrexian artifact creature.)\nDraw a card."

    spell {
        effect = Effects.Incubate(3) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Cristi Balanescu"
        flavorText = "No longer bound by the myopic limitations of flesh, Ekken would truly see the realms."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4303347-3fbd-4bbd-ab3f-e7ddbe0e0a9d.jpg?1783917037"
    }
}
