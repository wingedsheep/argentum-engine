package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

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

    additionalCost(AdditionalCost.SacrificePermanent(GameObjectFilter.Creature))

    spell {
        val t = target("target", TargetOpponent())
        effect = DealDamageEffect(
            amount = DynamicAmount.SacrificedPermanentPower,
            target = t
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "94"
        artist = "Andrew Robinson"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecdfcb03-2f77-4f54-af62-3012cd3efd4f.jpg"
    }
}
