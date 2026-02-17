package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CastTimeCreatureTypeSource
import com.wingedsheep.sdk.model.Rarity

/**
 * Aphetto Dredging
 * {3}{B}
 * Sorcery
 * Return up to three target creature cards of the creature type of your choice
 * from your graveyard to your hand.
 */
val AphettoDredging = card("Aphetto Dredging") {
    manaCost = "{3}{B}"
    typeLine = "Sorcery"
    oracleText = "Return up to three target creature cards of the creature type of your choice from your graveyard to your hand."

    castTimeCreatureTypeChoice = CastTimeCreatureTypeSource.GRAVEYARD

    spell {
        effect = EffectPatterns.chooseCreatureTypeReturnFromGraveyard(count = 3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Monte Michael Moore"
        flavorText = "Phage became both executioner and savior, helping others to the same rebirth she had found."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4e7fadf-40f1-45ff-97ef-5830381accc9.jpg?1562941515"
    }
}
