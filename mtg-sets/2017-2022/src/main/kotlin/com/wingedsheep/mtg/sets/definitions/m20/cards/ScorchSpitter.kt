package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scorch Spitter
 * {R}
 * Creature — Elemental Lizard
 * 1/1
 *
 * Whenever this creature attacks, it deals 1 damage to the player or planeswalker it's attacking.
 *
 * The damage goes to Scorch Spitter's declared defender ([EffectTarget.AttackedBy] of Self, the
 * Myr Battlesphere shape) — nothing when it attacks a battle.
 */
val ScorchSpitter = card("Scorch Spitter") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Lizard"
    power = 1
    toughness = 1
    oracleText = "Whenever this creature attacks, it deals 1 damage to the player or planeswalker it's attacking."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.DealDamage(1, EffectTarget.AttackedBy(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Johan Grenier"
        flavorText = "\"Yes, she can be a nuisance. But at least my tea never gets cold.\"\n—Marlan Farthwald, innkeeper"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb701a84-24bd-41ed-9f06-25c8338902a5.jpg?1783932970"
    }
}
