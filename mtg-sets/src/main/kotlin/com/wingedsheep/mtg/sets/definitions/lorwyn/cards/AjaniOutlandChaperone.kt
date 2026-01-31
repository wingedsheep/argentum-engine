package com.wingedsheep.mtg.sets.definitions.lorwyn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.CreateTokenEffect
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LookAtTopXPutOntoBattlefieldEffect
import com.wingedsheep.sdk.targeting.CreatureTargetFilter
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Ajani, Outland Chaperone
 * {1}{W}{W}
 * Legendary Planeswalker — Ajani
 * Loyalty: 3
 * +1: Create a 1/1 green and white Kithkin creature token.
 * −2: Ajani deals 4 damage to target tapped creature.
 * −8: Look at the top X cards of your library, where X is your life total.
 *     You may put any number of nonland permanent cards with mana value 3 or less
 *     from among them onto the battlefield. Then shuffle.
 */
val AjaniOutlandChaperone = card("Ajani, Outland Chaperone") {
    manaCost = "{1}{W}{W}"
    typeLine = "Legendary Planeswalker — Ajani"
    startingLoyalty = 3

    // +1: Create a 1/1 green and white Kithkin creature token.
    loyaltyAbility(+1) {
        effect = CreateTokenEffect(
            count = 1,
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Kithkin")
        )
    }

    // −2: Ajani deals 4 damage to target tapped creature.
    loyaltyAbility(-2) {
        target = TargetCreature(filter = CreatureTargetFilter.Tapped)
        effect = DealDamageEffect(
            amount = 4,
            target = EffectTarget.ContextTarget(0)
        )
    }

    // −8: Look at the top X cards of your library, where X is your life total.
    //     You may put any number of nonland permanent cards with mana value 3 or less
    //     from among them onto the battlefield. Then shuffle.
    loyaltyAbility(-8) {
        effect = LookAtTopXPutOntoBattlefieldEffect(
            countSource = DynamicAmount.YourLifeTotal,
            filter = CardFilter.And(
                listOf(
                    CardFilter.NonlandPermanentCard,
                    CardFilter.ManaValueAtMost(3)
                )
            ),
            shuffleAfter = true
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "4"
        artist = "Daren Bader"
    }
}
