package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Morkrut Behemoth
 * {4}{B}
 * Creature — Zombie Giant
 * 7/6
 * As an additional cost to cast this spell, sacrifice a creature or pay {1}{B}.
 * Menace
 *
 * Same "sacrifice a creature or pay {N}" shape as Eaten Alive ([Costs.additional.SacrificeOrPay]);
 * with no creature to sacrifice only the pay path is offered.
 */
val MorkrutBehemoth = card("Morkrut Behemoth") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Giant"
    power = 7
    toughness = 6
    oracleText = "As an additional cost to cast this spell, sacrifice a creature or pay {1}{B}.\n" +
        "Menace (This creature can't be blocked except by two or more creatures.)"

    additionalCost(
        Costs.additional.SacrificeOrPay(
            filter = GameObjectFilter.Creature,
            alternativeManaCost = "{1}{B}",
        )
    )

    keywords(Keyword.MENACE)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "114"
        artist = "Milivoj Ćeran"
        flavorText = "\"Let's see my brother's stitched-up little toys compete with this.\"\n—Ghoulcaller Gisa"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/318fe8a2-a68a-4c7e-9581-ae6800826164.jpg?1783925611"
    }
}
