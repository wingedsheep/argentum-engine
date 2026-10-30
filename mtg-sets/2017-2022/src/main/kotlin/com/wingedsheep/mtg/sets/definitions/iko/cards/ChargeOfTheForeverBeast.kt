package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Charge of the Forever-Beast
 * {2}{G}
 * Sorcery
 * As an additional cost to cast this spell, reveal a creature card from your hand.
 * Charge of the Forever-Beast deals damage to target creature or planeswalker equal to the
 * revealed card's power.
 *
 * The revealed card stays in hand (revealing moves nothing); `EffectTarget.RevealedAsCost` reads
 * its power as captured when the cost was paid, which is also its last-known power in hand if it
 * left before resolution (Titan's Presence shape).
 */
val ChargeOfTheForeverBeast = card("Charge of the Forever-Beast") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, reveal a creature card from your hand.\n" +
        "Charge of the Forever-Beast deals damage to target creature or planeswalker equal to the revealed card's power."

    additionalCost(Costs.additional.RevealFromHand(filter = GameObjectFilter.Creature))

    spell {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.powerOf(EffectTarget.RevealedAsCost()), victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Filip Burburan"
        flavorText = "The Zagoth wetlands are fertilized by centuries of Brokkos's former challengers."
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bf147a6-2282-4120-9cbd-d0309fc9b02b.jpg?1783931039"
        ruling("2020-04-17", "You must reveal exactly one card to cast Charge of the Forever-Beast; you can't cast it without revealing a card, and you can't reveal additional cards. The revealed card remains revealed until Charge of the Forever-Beast resolves.")
        ruling("2020-04-17", "As Charge of the Forever-Beast resolves, it deals damage based on the revealed card's power if it's still in your hand. If that card has left your hand, Charge of the Forever-Beast deals damage based on the card's power immediately before it left your hand.")
    }
}
