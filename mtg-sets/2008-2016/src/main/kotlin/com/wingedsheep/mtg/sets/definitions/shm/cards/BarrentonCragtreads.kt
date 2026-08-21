package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Barrenton Cragtreads
 * {2}{W/U}{W/U}
 * Creature — Kithkin Scout
 * 3 / 3
 *
 * This creature can't be blocked by red creatures.
 *
 * - The evasion is a single [CantBeBlockedBy] static over a coloured *creature* filter, not a
 *   keyword: the blocker restriction is checked against projected state, so a creature that has
 *   merely been *made* red (or has lost red) is judged by its current colour, not its printed one.
 * - The hybrid cost `{W/U}` goes in `manaCost` verbatim; mana value (4) is derived by the parser.
 */
val BarrentonCragtreads = card("Barrenton Cragtreads") {
    manaCost = "{2}{W/U}{W/U}"
    typeLine = "Creature — Kithkin Scout"
    power = 3
    toughness = 3
    oracleText = "This creature can't be blocked by red creatures."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withColor(Color.RED))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Daren Bader"
        flavorText = "\"Boggarts are easy to get around. Just toss some mutton in another direction. Giants are a little harder. You have to be quick to avoid their steps. Cinders are the difficult ones, but even they have fears to be exploited.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/6/361dab9e-22f0-45e4-a793-aa6c97a96781.jpg?1783942738"
    }
}
