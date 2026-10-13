package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Rona's Vortex
 * {U}
 * Instant
 * Kicker {2}{B}
 * Return target creature or planeswalker you don't control to its owner's hand. If this spell was
 * kicked, put that permanent on the bottom of its owner's library instead.
 */
val RonasVortex = card("Rona's Vortex") {
    manaCost = "{U}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Kicker {2}{B} (You may pay an additional {2}{B} as you cast this spell.)\nReturn target creature or planeswalker you don't control to its owner's hand. If this spell was kicked, put that permanent on the bottom of its owner's library instead."

    keywordAbility(KeywordAbility.kicker("{2}{B}"))

    spell {
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.withControllerPredicate(
                ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
            )))
        effect = Effects.If(
            condition = Conditions.WasKicked,
            then = Effects.PutOnBottomOfLibrary(victim),
            otherwise = Effects.ReturnToHand(victim)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdf96533-f28a-4465-893b-0c9394d5b8d7.jpg?1783921346"
    }
}
