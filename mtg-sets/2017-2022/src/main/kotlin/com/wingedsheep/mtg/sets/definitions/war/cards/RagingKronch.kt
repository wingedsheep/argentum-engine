package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Raging Kronch — War of the Spark #141
 * {2}{R} · Creature — Beast · 4/3
 *
 * This creature can't attack alone.
 *
 * "Can't attack alone" is the co-attacker restriction with the bare [GameObjectFilter.Creature]
 * filter: any other declared attacker satisfies it (as Loyal Pegasus's attack half).
 */
val RagingKronch = card("Raging Kronch") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 3
    oracleText = "This creature can't attack alone."

    staticAbility {
        ability = CantAttackUnlessCoAttacker(coAttackerFilter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Steve Prescott"
        flavorText = "\"Seeing so much easy prey makes her crazy. See how she snorts and drools?\"\n" +
            "—Dhuuz, kronch wrangler"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae38aa2d-6c0e-409a-bfc7-ed4281457670.jpg?1783933422"
    }
}
