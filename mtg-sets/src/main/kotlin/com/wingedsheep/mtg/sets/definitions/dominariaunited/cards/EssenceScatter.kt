package com.wingedsheep.mtg.sets.definitions.dominariaunited.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Essence Scatter
 * {1}{U}
 * Instant
 * Counter target creature spell.
 */
val EssenceScatter = card("Essence Scatter") {
    manaCost = "{1}{U}"
    typeLine = "Instant"
    oracleText = "Counter target creature spell."

    spell {
        target("creature spell", Targets.CreatureSpell)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49"
        artist = "Anastasia Ovchinnikova"
        flavorText = "\"Phyrexians pollute everything they touch, so the solution is simple: don't let them touch anything.\"\n—Teferi"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7ad4441-e300-4267-bedb-4ae6a64f59cd.jpg?1673306711"
    }
}
