package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Loyal Pegasus
 * {W}
 * Creature — Pegasus
 * 2/1
 * Flying
 * This creature can't attack or block alone.
 *
 * "Can't attack or block alone" is the co-attacker restriction plus the co-blocker restriction,
 * both with the bare [GameObjectFilter.Creature] filter: any other declared attacker (or blocker)
 * satisfies it.
 */
val LoyalPegasus = card("Loyal Pegasus") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Pegasus"
    power = 2
    toughness = 1
    oracleText = "Flying\nThis creature can't attack or block alone."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantAttackUnlessCoAttacker(coAttackerFilter = GameObjectFilter.Creature)
    }

    staticAbility {
        ability = CantBlockUnlessCoBlocker(coBlockerFilter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "John Severin Brassell"
        flavorText = "\"Even if I should die in the saddle, she will not let me fall to the ground.\"\n" +
            "—Hypatia of the Winged Horse"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6b29f36-e84d-402e-9ab7-935fd2e7532d.jpg?1783939579"
    }
}
