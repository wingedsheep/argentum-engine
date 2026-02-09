package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LoseLifeEffect

/**
 * Thrashing Mudspawn
 * {3}{B}{B}
 * Creature — Beast
 * 4/4
 * Whenever Thrashing Mudspawn is dealt damage, you lose that much life.
 * Morph {1}{B}{B}
 */
val ThrashingMudspawn = card("Thrashing Mudspawn") {
    manaCost = "{3}{B}{B}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.TakesDamage
        effect = LoseLifeEffect(
            amount = DynamicAmount.TriggerDamageAmount,
            target = EffectTarget.Controller
        )
    }

    morph = "{1}{B}{B}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Thomas M. Baxa"
        flavorText = "\"It just obeys you. It doesn't like you.\""
        imageUri = "https://cards.scryfall.io/large/front/d/a/da84de0e-a4cd-4dff-8ee3-87c9debf0969.jpg?1562947056"
    }
}
