package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Long Lake Nuisance
 * {3}{U}
 * Creature — Bird
 * 3/1
 *
 * Flying
 * When this creature enters, recruit.
 */
val LongLakeNuisance = card("Long Lake Nuisance") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    oracleText = "Flying\n" +
        "When this creature enters, recruit. (Draw a card, then discard a card. If you discarded " +
        "a nonland card, create a 1/1 white Human Soldier creature token.)"
    power = 3
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Mechanic.recruit()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Kevin Sidharta"
        flavorText = "Thrushes carried news between towns and villages. Herons, however, brought " +
            "nothing more than disruption."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd5af94d-6321-4834-8e5f-e5d0261b3ef3.jpg?1785497055"
    }
}
