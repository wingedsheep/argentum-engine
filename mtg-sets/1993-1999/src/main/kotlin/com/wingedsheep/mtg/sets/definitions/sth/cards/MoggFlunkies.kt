package com.wingedsheep.mtg.sets.definitions.sth.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Mogg Flunkies
 * {1}{R}
 * Creature — Goblin
 * 3/3
 * This creature can't attack or block alone.
 *
 * "Can't attack or block alone" is the co-attacker restriction plus the co-blocker restriction,
 * both with the bare [GameObjectFilter.Creature] filter: any other declared attacker (or blocker)
 * satisfies it.
 */
val MoggFlunkies = card("Mogg Flunkies") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    power = 3
    toughness = 3
    oracleText = "This creature can't attack or block alone."

    staticAbility {
        ability = CantAttackUnlessCoAttacker(coAttackerFilter = GameObjectFilter.Creature)
    }

    staticAbility {
        ability = CantBlockUnlessCoBlocker(coBlockerFilter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Brom"
        flavorText = "They'll attack whatever's in front of them—as long as you tell them where that is."
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b1b384e-a7d5-4b5f-87d5-95ac1a6c6320.jpg?1783946554"
        ruling("2018-03-16", "If you control more than one Mogg Flunkies, they can attack or block together, even if no other creatures attack or block.")
        ruling("2018-03-16", "Although Mogg Flunkies can't attack alone, other attacking creatures don't have to attack the same player, planeswalker, or battle. For example, Mogg Flunkies could attack an opponent and another creature could attack a planeswalker that opponent controls. Similarly, other blocking creatures don't have to block the same creature that Mogg Flunkies blocks.")
        ruling("2018-03-16", "Once Mogg Flunkies has attacked or blocked, it will remain in combat even if you no longer control another attacking or blocking creature.")
        ruling("2018-03-16", "If an effect says that Mogg Flunkies attacks or blocks if able and you control another creature able to attack or block, you must attack or block with Mogg Flunkies and that creature.")
        ruling("2018-03-16", "In a Two-Headed Giant game, Mogg Flunkies can attack or block with a creature controlled by your teammate, even if no other creatures you control are attacking or blocking.")
    }
}
