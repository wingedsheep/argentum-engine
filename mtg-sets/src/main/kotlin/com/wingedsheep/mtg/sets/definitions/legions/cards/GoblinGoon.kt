package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessControlMoreCreatures
import com.wingedsheep.sdk.scripting.CantBlockUnlessControlMoreCreatures

/**
 * Goblin Goon
 * {3}{R}
 * Creature — Goblin Mutant
 * 6/6
 * Goblin Goon can't attack unless you control more creatures than defending player.
 * Goblin Goon can't block unless you control more creatures than attacking player.
 */
val GoblinGoon = card("Goblin Goon") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Goblin Mutant"
    power = 6
    toughness = 6
    oracleText = "Goblin Goon can't attack unless you control more creatures than defending player.\nGoblin Goon can't block unless you control more creatures than attacking player."

    staticAbility {
        ability = CantAttackUnlessControlMoreCreatures()
    }

    staticAbility {
        ability = CantBlockUnlessControlMoreCreatures()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Greg Staples"
        flavorText = "Giant-sized body. Goblin-sized brain."
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c77cac8-fe95-4925-a815-8c514cc41b22.jpg?1562916759"
    }
}
