package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mudbrawler Raiders
 * {2}{R/G}{R/G}
 * Creature — Goblin Warrior
 * 3 / 3
 *
 * This creature can't be blocked by blue creatures.
 *
 * - The evasion is a single [CantBeBlockedBy] static over a coloured *creature* filter, evaluated
 *   against projected state so a blocker's current colour decides, not its printed one.
 * - The hybrid cost `{R/G}` goes in `manaCost` verbatim; mana value (4) is derived by the parser.
 */
val MudbrawlerRaiders = card("Mudbrawler Raiders") {
    manaCost = "{2}{R/G}{R/G}"
    typeLine = "Creature — Goblin Warrior"
    power = 3
    toughness = 3
    oracleText = "This creature can't be blocked by blue creatures."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withColor(Color.BLUE))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "213"
        artist = "Ron Spencer"
        flavorText = "To reach the ravine of the Wanderbrine River, they were told to take the shortcut \"through the mountains.\" They took the directions literally."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cbe4af47-d913-4c19-a027-501e2c78758c.jpg?1783942721"
    }
}
