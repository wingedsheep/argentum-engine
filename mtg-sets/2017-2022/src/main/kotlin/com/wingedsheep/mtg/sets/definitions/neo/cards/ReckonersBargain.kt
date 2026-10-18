package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Reckoner's Bargain — Kamigawa: Neon Dynasty #120 (canonical printing)
 * {1}{B} · Instant
 *
 * As an additional cost to cast this spell, sacrifice an artifact or creature.
 * You gain life equal to the sacrificed permanent's mana value. Draw two cards.
 *
 * The life amount reads the sacrificed permanent's mana value as it was paid
 * ([DynamicAmounts.sacrificedManaValue]), the same cost-linked amount Sacrifice (LEA) uses.
 */
val ReckonersBargain = card("Reckoner's Bargain") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature.\n" +
        "You gain life equal to the sacrificed permanent's mana value. Draw two cards."

    additionalCost(Costs.additional.SacrificePermanent(GameObjectFilter.Artifact or GameObjectFilter.Creature))

    spell {
        effect = Effects.GainLife(DynamicAmounts.sacrificedManaValue()) then
            Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Justine Cruz"
        flavorText = "Restricted auctions are held once a year at the Dragon's Lair—the sacred, neutral " +
            "meeting ground of all Reckoner gangs."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/6338942d-d650-4571-8ec6-4d658792c53e.jpg?1783923877"
    }
}
