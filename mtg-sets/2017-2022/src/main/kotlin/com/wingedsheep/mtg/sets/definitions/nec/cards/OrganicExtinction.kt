package com.wingedsheep.mtg.sets.definitions.nec.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Organic Extinction — Neon Dynasty Commander #8 (canonical printing)
 * {8}{W}{W} · Sorcery
 *
 * Improvise
 * Destroy all nonartifact creatures.
 */
val OrganicExtinction = card("Organic Extinction") {
    manaCost = "{8}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Improvise (Your artifacts can help cast this spell. Each artifact you tap after " +
        "you're done activating mana abilities pays for {1}.)\nDestroy all nonartifact creatures."

    keywords(Keyword.IMPROVISE)

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Creature.nonartifact())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "8"
        artist = "Cristi Balanescu"
        flavorText = "\"Technology evolves faster than a blade can swing.\"\n—Arima, lead inventor of the Futurists"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fea0f8be-c242-49dd-bae3-0b306107ac0b.jpg?1783923996"
    }
}
