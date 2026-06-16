package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Prismari Charm
 * {U}{R}
 * Instant
 * Choose one —
 * • Surveil 2, then draw a card.
 * • Prismari Charm deals 1 damage to each of one or two targets.
 * • Return target nonland permanent to its owner's hand.
 */
val PrismariCharm = card("Prismari Charm") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Surveil 2, then draw a card.\n• Prismari Charm deals 1 damage to each of one or two targets.\n• Return target nonland permanent to its owner's hand."

    spell {
        modal(chooseCount = 1) {
            mode("Surveil 2, then draw a card") {
                effect = Patterns.Library.surveil(2) then Effects.DrawCards(1)
            }
            mode("Prismari Charm deals 1 damage to each of one or two targets") {
                target = AnyTarget(count = 2, minCount = 1)
                effect = ForEachTargetEffect(
                    effects = listOf(Effects.DealDamage(1, EffectTarget.ContextTarget(0)))
                )
            }
            mode("Return target nonland permanent to its owner's hand") {
                val t = target("target nonland permanent", Targets.NonlandPermanent)
                effect = Effects.ReturnToHand(t)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "211"
        artist = "Inkognit"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f6c2a5e-fe13-407c-aadd-c9caf2884ff1.jpg?1775938465"
    }
}
