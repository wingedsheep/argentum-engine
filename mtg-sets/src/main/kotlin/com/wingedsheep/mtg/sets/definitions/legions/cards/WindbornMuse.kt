package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackTax

/**
 * Windborn Muse
 * {3}{W}
 * Creature — Spirit
 * 2/3
 * Flying
 * Creatures can't attack you unless their controller pays {2} for each creature
 * they control that's attacking you.
 */
val WindbornMuse = card("Windborn Muse") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 3
    oracleText = "Flying\nCreatures can't attack you unless their controller pays {2} for each creature they control that's attacking you."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = AttackTax(manaCostPerAttacker = "{2}")
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Adam Rex"
        flavorText = "\"Her voice is justice, clear and relentless.\" —Akroma, angelic avenger"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c45fd87-7b44-4e1a-b30f-41220b69d9e6.jpg?1562916728"
    }
}
