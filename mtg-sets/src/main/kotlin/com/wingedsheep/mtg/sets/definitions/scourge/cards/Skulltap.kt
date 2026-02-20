package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Skulltap
 * {1}{B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a creature.
 * Draw two cards.
 */
val Skulltap = card("Skulltap") {
    manaCost = "{1}{B}"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice a creature.\nDraw two cards."

    additionalCost(AdditionalCost.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Carl Critchlow"
        flavorText = "\"I can teach you much about the secrets of the grave—but first you must see one from the inside.\""
        imageUri = "https://cards.scryfall.io/large/front/4/8/48a90779-008e-401f-9877-be0a935d2ccd.jpg?1562528514"
    }
}
