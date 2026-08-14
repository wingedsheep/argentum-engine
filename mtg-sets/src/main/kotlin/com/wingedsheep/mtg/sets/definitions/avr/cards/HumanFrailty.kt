package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Human Frailty
 * {B}
 * Instant
 * Destroy target Human creature.
 */
val HumanFrailty = card("Human Frailty") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy target Human creature."

    spell {
        val t = target(
            "Human creature",
            TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN))),
        )
        effect = Effects.Destroy(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "109"
        artist = "David Palumbo"
        flavorText =
            "\"I have seen a hundred mortal families rise and fall. I shall outlast a thousand more.\"\n—Olivia Voldaren"
        imageUri =
            "https://cards.scryfall.io/normal/front/1/d/1d1de712-86ac-4c03-be86-2403cd121f66.jpg?1783940696"
    }
}
