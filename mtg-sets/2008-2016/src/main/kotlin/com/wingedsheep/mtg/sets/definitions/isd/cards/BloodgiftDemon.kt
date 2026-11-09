package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Bloodgift Demon
 * {3}{B}{B}
 * Creature — Demon
 * 5/4
 * Flying
 * At the beginning of your upkeep, target player draws a card and loses 1 life.
 */
val BloodgiftDemon = card("Bloodgift Demon") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    oracleText = "Flying\nAt the beginning of your upkeep, target player draws a card and loses 1 life."
    power = 5
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val player = target(Targets.Player)
        effect = Effects.DrawCards(1, player) then Effects.LoseLife(1, player)
        description = "At the beginning of your upkeep, target player draws a card and loses 1 life."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Peter Mohrbacher"
        flavorText = "He relishes the devotion of his Skirsdag puppets and their belief that it will earn them immortality."
        imageUri =
            "https://cards.scryfall.io/normal/front/f/2/f271addb-e267-4397-b181-f1eaeabbfe71.jpg?1783940960"
    }
}
