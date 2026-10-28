package com.wingedsheep.mtg.sets.definitions.nem.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost

/**
 * Reverent Silence
 * {3}{G}
 * Sorcery
 * If you control a Forest, rather than pay this spell's mana cost, you may have each other player gain 6 life.
 * Destroy all enchantments.
 */
val ReverentSilence = card("Reverent Silence") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "If you control a Forest, rather than pay this spell's mana cost, you may have each other player gain 6 life.\n" +
        "Destroy all enchantments."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.parse("{0}"),
        additionalCosts = listOf(Costs.additional.OthersGainLife(6, eachOtherPlayer = true)),
        condition = Conditions.YouControl(GameObjectFilter.Land.withSubtype("Forest")),
    )

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Enchantment)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "111"
        artist = "Don Hazeltine"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b82d3432-2167-4a65-8221-cb7b338e60d0.jpg?1783945815"
    }
}
