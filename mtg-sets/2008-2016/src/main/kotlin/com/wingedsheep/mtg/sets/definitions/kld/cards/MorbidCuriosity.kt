package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Morbid Curiosity — Kaladesh #94 (canonical printing)
 * {1}{B}{B} · Sorcery
 *
 * As an additional cost to cast this spell, sacrifice an artifact or creature.
 * Draw cards equal to the mana value of the sacrificed permanent.
 *
 * The draw count reads the sacrificed permanent's mana value as it was paid
 * ([DynamicAmounts.sacrificedManaValue]), the cost-linked amount Reckoner's Bargain uses.
 */
val MorbidCuriosity = card("Morbid Curiosity") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature.\n" +
        "Draw cards equal to the mana value of the sacrificed permanent."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact or GameObjectFilter.Creature))

    spell {
        effect = Effects.DrawCards(DynamicAmounts.sacrificedManaValue())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Kieran Yanner"
        flavorText = "\"Every creation carries a piece of its creators. Their knowledge, their dreams, " +
            "their soul. That is the best part.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f26722e-e7fc-4a90-9a5b-cefda096e5fe.jpg?1783937203"
        ruling("2016-09-20", "If a permanent has {X} in its mana cost, X is considered to be 0.")
        ruling("2016-09-20", "To determine how many cards to draw, use the mana value of the permanent immediately before you sacrificed it.")
    }
}
