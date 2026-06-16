package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Boneyard Desecrator
 * {3}{B}
 * Creature — Zombie Mercenary
 * 3/4
 *
 * Menace
 * {1}{B}, Sacrifice another creature: Put a +1/+1 counter on this creature. If an outlaw
 * was sacrificed this way, create a Treasure token. (Assassins, Mercenaries, Pirates,
 * Rogues, and Warlocks are outlaws.)
 *
 * The "if an outlaw was sacrificed this way" rider reads the cost-sacrificed permanent's
 * snapshot via the existing [Conditions.SacrificedHadSubtype] condition (which inspects
 * `EffectContext.sacrificedPermanents`, captured at cost-payment time). "Outlaw" is any of
 * the five OTJ outlaw creature types, so the gate is an OR ([Conditions.Any]) over
 * [Subtype.OUTLAW_TYPES] — no card-specific condition needed.
 */
val BoneyardDesecrator = card("Boneyard Desecrator") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Mercenary"
    power = 3
    toughness = 4
    oracleText = "Menace\n" +
        "{1}{B}, Sacrifice another creature: Put a +1/+1 counter on this creature. " +
        "If an outlaw was sacrificed this way, create a Treasure token. " +
        "(Assassins, Mercenaries, Pirates, Rogues, and Warlocks are outlaws.)"

    keywords(Keyword.MENACE)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature)
        )
        effect = Effects.AddCounters("+1/+1", 1, EffectTarget.Self)
            .then(
                ConditionalEffect(
                    condition = Conditions.Any(
                        *Subtype.OUTLAW_TYPES
                            .map { Conditions.SacrificedHadSubtype(it.value) }
                            .toTypedArray()
                    ),
                    effect = Effects.CreateTreasure(1)
                )
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Maxime Minard"
        flavorText = "It's never too late to start an afterlife of crime."
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d43981b1-60c8-4896-8885-b07e73b99b30.jpg?1712355558"
    }
}
