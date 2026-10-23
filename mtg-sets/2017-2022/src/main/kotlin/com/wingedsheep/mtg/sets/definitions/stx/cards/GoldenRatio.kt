package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Golden Ratio — Strixhaven: School of Mages #190
 * {1}{G}{U} · Sorcery
 *
 * Draw a card for each different power among creatures you control.
 *
 * The count is the number of distinct projected powers among creatures you control
 * ([com.wingedsheep.sdk.scripting.values.Aggregation.DISTINCT_VALUES] over
 * [CardNumericProperty.POWER]), read as the spell resolves.
 */
val GoldenRatio = card("Golden Ratio") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Sorcery"
    oracleText = "Draw a card for each different power among creatures you control."

    spell {
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature)
                .distinctValues(CardNumericProperty.POWER)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Alix Branwyn"
        flavorText = "\"When I look around, I see a world defined by patterns that still manages to defy them.\"\n" +
            "—Kianne, Quandrix dean"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/626fbddb-3963-4dd2-8507-9777ed66bbbc.jpg?1783927312"
        ruling(
            "2021-04-16",
            "Creatures have different power if their powers are not equal. For example, if you have a creature " +
                "with 2 power, two creatures with 1 power, and a creature with -1 power, you will draw three " +
                "cards as Golden Ratio resolves.",
        )
    }
}
