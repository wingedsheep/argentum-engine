package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Rotting Regisaur (M20 #111)
 * {2}{B}  Creature — Zombie Dinosaur  7/6
 *
 * At the beginning of your upkeep, discard a card.
 */
val RottingRegisaur = card("Rotting Regisaur") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Dinosaur"
    oracleText = "At the beginning of your upkeep, discard a card."
    power = 7
    toughness = 6

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Patterns.Hand.discardCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Randy Vargas"
        flavorText = "\"Now, *that's* a zombie!\"\n—Modriss of Zargoth Fen"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d88c61cb-dbf9-46c3-8795-8896ba1208d5.jpg"
    }
}
