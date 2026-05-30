package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vicious Kavu
 * {1}{B}{R}
 * Creature — Kavu
 * 2/2
 * Whenever this creature attacks, it gets +2/+0 until end of turn.
 */
val ViciousKavu = card("Vicious Kavu") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Kavu"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature attacks, it gets +2/+0 until end of turn."

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.ModifyStats(power = 2, toughness = 0, target = EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "284"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31e9e629-7c25-4d45-aa35-9ba5f95b43cb.jpg?1562905117"
    }
}
