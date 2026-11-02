package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mentor
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Wojek Bodyguard
 * {2}{R}
 * Creature — Human Soldier
 * 3/3
 *
 * Mentor
 * This creature can't attack or block alone.
 *
 * "Can't attack or block alone" is the co-attacker plus co-blocker restriction with the bare
 * [GameObjectFilter.Creature] filter, as on Loyal Pegasus.
 */
val WojekBodyguard = card("Wojek Bodyguard") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Mentor (Whenever this creature attacks, put a +1/+1 counter on target attacking creature with " +
        "lesser power.)\nThis creature can't attack or block alone."

    mentor()

    staticAbility {
        ability = CantAttackUnlessCoAttacker(coAttackerFilter = GameObjectFilter.Creature)
    }

    staticAbility {
        ability = CantBlockUnlessCoBlocker(coBlockerFilter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Tyler Walpole"
        flavorText = "\"By all means, take another step toward the captain.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebff5313-e325-47c3-814b-38fdb4c6c8d2.jpg?1783934155"
    }
}
