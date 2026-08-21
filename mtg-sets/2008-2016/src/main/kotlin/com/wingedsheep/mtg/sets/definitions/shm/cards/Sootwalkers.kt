package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Sootwalkers
 * {2}{B/R}{B/R}
 * Creature — Elemental Rogue
 * 3 / 3
 *
 * This creature can't be blocked by white creatures.
 *
 * - The evasion is a single [CantBeBlockedBy] static over a coloured *creature* filter, evaluated
 *   against projected state so a blocker's current colour decides, not its printed one.
 * - The hybrid cost `{B/R}` goes in `manaCost` verbatim; mana value (4) is derived by the parser.
 */
val Sootwalkers = card("Sootwalkers") {
    manaCost = "{2}{B/R}{B/R}"
    typeLine = "Creature — Elemental Rogue"
    power = 3
    toughness = 3
    oracleText = "This creature can't be blocked by white creatures."

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.withColor(Color.WHITE))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Nils Hamm"
        flavorText = "\"Why allow the fires of others to burn, when ours do not? Why leave them content, while we suffer? If there is to be misery, let it be borne by all.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f36c7932-cc1e-4b1f-ae39-2f04b84bcddb.jpg?1783942724"
    }
}
