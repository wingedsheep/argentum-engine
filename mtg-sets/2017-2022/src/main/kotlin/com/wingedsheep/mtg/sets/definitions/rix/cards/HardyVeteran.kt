package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Hardy Veteran
 * {1}{G}
 * Creature — Human Warrior
 * 2/2
 * During your turn, this creature gets +0/+2.
 */
val HardyVeteran = card("Hardy Veteran") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    oracleText = "During your turn, this creature gets +0/+2."
    power = 2
    toughness = 2

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(0, 2, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Suzanne Helmigh"
        flavorText = "For the Sun Empire, the Immortal Sun is a symbol of national identity. When they reclaim it, the Empire will flourish once more."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62f2755f-276f-4260-a201-f02af88b5708.jpg?1783935286"

        ruling(
            "2018-01-19",
            "Hardy Veteran gets +0/+2 for the entire duration of your turn. If it's dealt damage or " +
                "gets -X/-X until end of turn, those will wear off before your turn is over."
        )
    }
}
