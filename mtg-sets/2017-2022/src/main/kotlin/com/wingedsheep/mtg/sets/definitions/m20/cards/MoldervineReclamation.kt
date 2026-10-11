package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Moldervine Reclamation
 * {3}{B}{G}
 * Enchantment
 *
 * Whenever a creature you control dies, you gain 1 life and draw a card.
 */
val MoldervineReclamation = card("Moldervine Reclamation") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control dies, you gain 1 life and draw a card."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        effect = Effects.GainLife(1) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Antonio José Manzanedo"
        flavorText = "\"The heroes of the past nourish our spirits by their example—and nourish our crops by their decay.\"\n—Jeddeg, philosopher of graves"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/618d21fe-8e3d-4887-b2e4-b92194ba1902.jpg"
    }
}
