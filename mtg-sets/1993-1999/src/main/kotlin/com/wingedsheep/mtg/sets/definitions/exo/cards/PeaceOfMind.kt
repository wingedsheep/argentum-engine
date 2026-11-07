package com.wingedsheep.mtg.sets.definitions.exo.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Peace of Mind (Exodus #13)
 * {1}{W}
 * Enchantment
 *
 * {W}, Discard a card: You gain 3 life.
 */
val PeaceOfMind = card("Peace of Mind") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "{W}, Discard a card: You gain 3 life."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Discard())
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Randy Elliott"
        flavorText = "Three are the paths of the soul:\nOne out—roam across the plain,\nOne back—dwell with tribe and tent,\nOne in—turn an enemy's hate."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c383f12f-da06-4ef0-bf8e-6a8a9cfcc74c.jpg"
    }
}
