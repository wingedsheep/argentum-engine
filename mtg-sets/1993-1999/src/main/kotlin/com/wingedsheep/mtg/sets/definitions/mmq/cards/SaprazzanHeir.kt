package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Saprazzan Heir
 * {1}{U}
 * Creature — Merfolk
 * 1 / 1
 *
 * Whenever this creature becomes blocked, you may draw three cards.
 */
val SaprazzanHeir = card("Saprazzan Heir") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk"
    oracleText = "Whenever this creature becomes blocked, you may draw three cards."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.BecomesBlocked
        optional = true
        effect = Effects.DrawCards(3)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Terese Nielsen"
        flavorText = "Within the walls of the floating city, Saprazzan wealth is like Saprazzan water: abundant, free-flowing, and accessible to all."
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e3d913d-2dcf-4747-8169-0c44ec895864.jpg"
    }
}
