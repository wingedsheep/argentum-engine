package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Wanderbrine Rootcutters
 * {2}{U/B}{U/B}
 * Creature — Merfolk Rogue
 * 3 / 3
 *
 * This creature can't be blocked by green creatures.
 *
 * - The evasion is a single [CantBeBlockedBy] static over a coloured *creature* filter, evaluated
 *   against projected state so a blocker's current colour decides, not its printed one.
 * - The hybrid cost `{U/B}` goes in `manaCost` verbatim; mana value (4) is derived by the parser.
 */
val WanderbrineRootcutters = card("Wanderbrine Rootcutters") {
    manaCost = "{2}{U/B}{U/B}"
    typeLine = "Creature — Merfolk Rogue"
    power = 3
    toughness = 3
    oracleText = "This creature can't be blocked by green creatures."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withColor(Color.GREEN))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "178"
        artist = "Chippy"
        flavorText = "Most dirtwalkers only know of the vicious merrows that dwell in the shallows. They can't begin to fathom the wickedness that skulks in the Dark Meanders."
        imageUri = "https://cards.scryfall.io/normal/front/a/b/ab739d64-cdcd-4f07-9854-e067c37c4f41.jpg?1783942728"
    }
}
