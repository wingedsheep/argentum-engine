package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rat Colony
 * {1}{B}
 * Creature — Rat
 * 2/1
 * Rat Colony gets +1/+0 for each other Rat you control.
 * A deck can have any number of cards named Rat Colony.
 */
val RatColony = card("Rat Colony") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Rat"
    power = 2
    toughness = 1
    oracleText = "Rat Colony gets +1/+0 for each other Rat you control.\nA deck can have any number of cards named Rat Colony."

    staticAbility {
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmount.AggregateBattlefield(
                Player.You,
                GameObjectFilter.Creature.withSubtype("Rat"),
                excludeSelf = true
            ),
            toughnessBonus = DynamicAmount.Fixed(0)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Suzanne Helmigh"
        flavorText = "\"Wreckage from the Phyrexian Invasion provided the rats with a seemingly unlimited number of breeding grounds.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f618e07-f06f-45d2-8512-e6cef88c0434.jpg?1739485440"
    }
}
