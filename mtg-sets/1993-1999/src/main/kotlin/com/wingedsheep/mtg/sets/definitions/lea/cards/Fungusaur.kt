package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fungusaur — Limited Edition Alpha #195
 * {3}{G} · Creature — Fungus Dinosaur · 2/2
 *
 * Whenever this creature is dealt damage, put a +1/+1 counter on it.
 *
 * "Is dealt damage" triggers once per damage event, so two blockers hitting it at once grow it by
 * one counter, not two (its ruling) — the bare `Triggers.self.isDealtDamage()`.
 */
val Fungusaur = card("Fungusaur") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus Dinosaur"
    oracleText = "Whenever this creature is dealt damage, put a +1/+1 counter on it."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "195"
        artist = "Daniel Gelon"
        flavorText = "Rather than sheltering her young, the female Fungusaur often injures her own offspring, " +
            "thereby ensuring their rapid growth."
        imageUri = "https://cards.scryfall.io/normal/front/5/a/5ad89f0d-b09b-40a0-84d6-3ee60dec7e23.jpg"
        ruling("2004-10-04", "If more than one creature damages it at one time, it only gets one counter.")
    }
}
