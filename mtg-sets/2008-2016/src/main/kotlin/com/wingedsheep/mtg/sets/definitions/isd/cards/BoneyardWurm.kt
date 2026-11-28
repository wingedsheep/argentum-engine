package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Boneyard Wurm
 * {1}{G}
 * Creature — Wurm
 * * / *
 * Boneyard Wurm's power and toughness are each equal to the number of creature cards in your graveyard.
 */
val BoneyardWurm = card("Boneyard Wurm") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    oracleText = "Boneyard Wurm's power and toughness are each equal to the number of creature cards in your graveyard."
    dynamicStats(DynamicAmounts.creatureCardsInYourGraveyard())
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "Jaime Jones"
        flavorText = "The only thing it likes more than discovering a pit of bones is adding to it."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75f3d9eb-462c-41b5-ad1a-baab7dc5eac3.jpg"
    }
}
