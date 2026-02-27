package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.Aggregation
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Ancient Ooze
 * {5}{G}{G}
 * Creature — Ooze
 * Ancient Ooze's power and toughness are each equal to the total mana value
 * of other creatures you control.
 */
val AncientOoze = card("Ancient Ooze") {
    manaCost = "{5}{G}{G}"
    typeLine = "Creature — Ooze"
    oracleText = "Ancient Ooze's power and toughness are each equal to the total mana value of other creatures you control."

    dynamicStats(
        DynamicAmount.AggregateBattlefield(
            player = Player.You,
            filter = GameObjectFilter.Creature,
            aggregation = Aggregation.SUM,
            property = CardNumericProperty.MANA_VALUE,
            excludeSelf = true
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "112"
        artist = "Erica Gassalasca-Jape"
        flavorText = "The ooze has always been. The ooze will always be."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b57b41c-f99c-4525-8541-f025b7e31974.jpg?1562527613"

        ruling("2004-10-04", "You add up the mana value of all other creatures you control. Remember that tokens and face-down creatures have a cost of zero.")
    }
}
