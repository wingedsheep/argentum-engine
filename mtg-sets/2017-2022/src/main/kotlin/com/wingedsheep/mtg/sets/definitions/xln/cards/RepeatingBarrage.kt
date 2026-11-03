package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Repeating Barrage
 * {1}{R}{R}
 * Sorcery
 * Repeating Barrage deals 3 damage to any target.
 * Raid — {3}{R}{R}: Return this card from your graveyard to your hand. Activate only if you attacked this turn.
 *
 * The raid ability is a graveyard-zone activated ability gated on [Conditions.YouAttackedThisTurn]
 * (same shape as Bloodsoaked Champion, returning to hand instead of the battlefield).
 */
val RepeatingBarrage = card("Repeating Barrage") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Repeating Barrage deals 3 damage to any target.\n" +
        "Raid — {3}{R}{R}: Return this card from your graveyard to your hand. Activate only if you attacked this turn."

    spell {
        val t = target(Targets.Any)
        effect = Effects.DealDamage(3, t)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{R}{R}")
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(Conditions.YouAttackedThisTurn)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "156"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba90a2d6-8292-4ff1-91d0-b30ae9775f12.jpg?1783935741"
    }
}
