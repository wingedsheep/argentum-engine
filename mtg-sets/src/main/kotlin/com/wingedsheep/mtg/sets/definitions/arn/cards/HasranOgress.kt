package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Costs

/**
 * Hasran Ogress
 * {B}{B}
 * Creature — Ogre
 * 3/2
 * Whenever this creature attacks, it deals 3 damage to you unless you pay {2}.
 */
val HasranOgress = card("Hasran Ogress") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Ogre"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature attacks, it deals 3 damage to you unless you pay {2}."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = PayOrSufferEffect(
            cost = Costs.pay.Mana(ManaCost.parse("{2}")),
            suffer = DealDamageEffect(3, EffectTarget.Controller),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f310cf5-0985-4826-9779-19a713089d6d.jpg?1562924693"
    }
}
