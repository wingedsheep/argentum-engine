package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pumpkin Bombardment — Marvel's Spider-Man #139
 * {B/R} · Sorcery
 *
 * As an additional cost to cast this spell, discard a card or pay {2}.
 * Pumpkin Bombardment deals 3 damage to target creature.
 *
 * The additional cost is the "discard a card or pay {N}" shape
 * ([Costs.additional.DiscardOrPay]); with an empty hand only the pay path is offered.
 */
val PumpkinBombardment = card("Pumpkin Bombardment") {
    manaCost = "{B/R}"
    colorIdentity = "BR"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, discard a card or pay {2}.\n" +
        "Pumpkin Bombardment deals 3 damage to target creature."

    additionalCost(
        Costs.additional.DiscardOrPay(alternativeManaCost = "{2}")
    )

    spell {
        target = Targets.Creature
        effect = Effects.DealDamage(3, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Leon Tukker"
        flavorText = "\"Happy Halloween, Spider-Man!\"\n—Green Goblin, Norman Osborn"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a268ad73-9a1f-47d9-9a85-a4669a769c3d.jpg?1783905315"
    }
}
