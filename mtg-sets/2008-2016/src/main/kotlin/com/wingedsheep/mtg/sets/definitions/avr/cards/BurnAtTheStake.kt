package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity

/**
 * Burn at the Stake
 * {2}{R}{R}{R}
 * Sorcery
 * As an additional cost to cast this spell, tap any number of untapped creatures you control.
 * Burn at the Stake deals damage to any target equal to three times the number of creatures
 * tapped this way.
 *
 * The creatures are tapped as the spell is cast (CR 601.2h); tapping none is a legal payment, and a
 * creature that entered this turn may be tapped (it's a cost, not the {T} symbol). The count is
 * fixed at payment, so a tapped creature that later leaves still counts.
 */
val BurnAtTheStake = card("Burn at the Stake") {
    manaCost = "{2}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, tap any number of untapped creatures you control.\n" +
        "Burn at the Stake deals damage to any target equal to three times the number of creatures tapped this way."

    additionalCost(Costs.additional.TapPermanentsVariable())

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(DynamicAmounts.permanentsTappedAsCost() * 3, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "130"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d46e9902-81fb-4a3c-9f2f-d3faf031631d.jpg?1783940688"
    }
}
