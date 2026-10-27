package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Craven Hulk
 * {3}{R}
 * Creature — Giant Coward
 * 4/4
 *
 * This creature can't block alone.
 *
 * "Can't block alone" is [CantBlockUnlessCoBlocker] with the bare [GameObjectFilter.Creature]
 * filter: any other creature declared as a blocker this combat satisfies it, whichever attacker it
 * blocks (CR 509.1b — checked against the whole proposed blocker group). Two Hulks satisfy each
 * other, and once declared the Hulk stays blocking even if its co-blocker leaves combat.
 */
val CravenHulk = card("Craven Hulk") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant Coward"
    power = 4
    toughness = 4
    oracleText = "This creature can't block alone."

    staticAbility {
        ability = CantBlockUnlessCoBlocker(coBlockerFilter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Nicholas Gregory"
        flavorText = "Abandoned at birth, the giant was raised by goats. Ironically, it grew up to be " +
            "the most cautious member of the herd."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2be804ab-1024-4eee-b339-48ed8b84d363.jpg?1783928234"
        ruling(
            "2021-02-05",
            "Although Craven Hulk can't be the only creature you declare as a blocking creature, it can be " +
                "the only creature that blocks any given attacking creature. Other blocking creatures don't " +
                "have to block the same creature Craven Hulk does."
        )
        ruling(
            "2021-02-05",
            "If you control more than one Craven Hulk, they can all block together, even if no other " +
                "creatures block."
        )
        ruling(
            "2021-02-05",
            "Once Craven Hulk has been declared as a blocking creature, it will remain in combat even if all " +
                "other blockers leave the battlefield or are otherwise removed from combat."
        )
    }
}
