package com.wingedsheep.mtg.sets.definitions.ody.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnlessCoAttacker
import com.wingedsheep.sdk.scripting.CantBlockUnlessCoBlocker
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ember Beast
 * {2}{R}
 * Creature — Beast
 * 3/4
 * This creature can't attack or block alone.
 */
val EmberBeast = card("Ember Beast") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Beast"
    power = 3
    toughness = 4
    oracleText = "This creature can't attack or block alone."

    staticAbility {
        ability = CantAttackUnlessCoAttacker(coAttackerFilter = GameObjectFilter.Creature)
    }

    staticAbility {
        ability = CantBlockUnlessCoBlocker(coBlockerFilter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Wayne England"
        flavorText = "Spot one ember beast, attack.\nSpot a second, best fall back.\n—Dwarven saying"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25080720-612f-40c0-8894-cda8e3e8afb8.jpg?1783945231"
    }
}
