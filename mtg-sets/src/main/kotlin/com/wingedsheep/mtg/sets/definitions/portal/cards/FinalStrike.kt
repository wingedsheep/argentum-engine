package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.CardFilter
import com.wingedsheep.sdk.scripting.DealDynamicDamageEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Final Strike
 * {2}{B}{B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice a creature.
 * Final Strike deals damage equal to the sacrificed creature's power
 * to target opponent or planeswalker.
 */
val FinalStrike = card("Final Strike") {
    manaCost = "{2}{B}{B}"
    typeLine = "Sorcery"

    additionalCost(AdditionalCost.SacrificePermanent(CardFilter.CreatureCard))

    spell {
        target = TargetOpponent()
        effect = DealDynamicDamageEffect(
            amount = DynamicAmount.SacrificedPermanentPower,
            target = EffectTarget.ContextTarget(0)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Andrew Robinson"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f947e25-fba6-4fea-a9c1-f5bf25f2dc35.jpg"
    }
}
