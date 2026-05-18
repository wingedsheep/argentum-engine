package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Urborg Drake
 * {1}{U}{B}
 * Creature — Drake
 * 2/3
 * Flying
 * This creature attacks each combat if able.
 */
val UrborgDrake = card("Urborg Drake") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Drake"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "This creature attacks each combat if able."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "283"
        artist = "Sam Wood"
        flavorText = "Relentless as the sea, remorseless as death."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97d1327e-bf87-423f-8a04-8124e45b9ae0.jpg?1562925655"
    }
}
