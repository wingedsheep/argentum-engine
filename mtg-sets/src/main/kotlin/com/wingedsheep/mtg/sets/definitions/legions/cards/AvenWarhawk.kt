package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AmplifyEffect

/**
 * Aven Warhawk
 * {4}{W}
 * Creature — Bird Soldier
 * 2/2
 * Amplify 1 (As this creature enters, put a +1/+1 counter on it for each
 * Bird and/or Soldier card you reveal in your hand.)
 * Flying
 */
val AvenWarhawk = card("Aven Warhawk") {
    manaCost = "{4}{W}"
    typeLine = "Creature — Bird Soldier"
    power = 2
    toughness = 2
    oracleText = "Amplify 1 (As this creature enters, put a +1/+1 counter on it for each Bird and/or Soldier card you reveal in your hand.)\nFlying"

    keywords(Keyword.AMPLIFY, Keyword.FLYING)

    replacementEffect(AmplifyEffect(countersPerReveal = 1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/normal/front/3/8/386a7062-6da8-4663-a218-75d894f7c0e0.jpg?1562906368"
    }
}
