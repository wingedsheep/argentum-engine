package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eiganjo Exemplar — Kamigawa: Neon Dynasty #10 (canonical printing)
 * {1}{W} · Enchantment Creature — Human Samurai · 2/1
 *
 * Whenever a Samurai or Warrior you control attacks alone, it gets +1/+1 until end of turn.
 *
 * "it" is the lone attacker, not the Exemplar: the ANY-bound `attacks` trigger binds the attacking
 * creature, so the pump goes to [EffectTarget.TriggeringEntity].
 */
val EiganjoExemplar = card("Eiganjo Exemplar") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Human Samurai"
    power = 2
    toughness = 1
    oracleText = "Whenever a Samurai or Warrior you control attacks alone, it gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.SAMURAI, Subtype.WARRIOR)).youControl()
        ).attacks(setOf(AttackPredicate.Alone))
        effect = Effects.ModifyStats(1, 1, EffectTarget.TriggeringEntity)
        description = "Whenever a Samurai or Warrior you control attacks alone, it gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "10"
        artist = "Ernanda Souza"
        flavorText = "Imperial samurai undergo strict training, honing their skills and purifying their " +
            "spirits in the hope that they will prove worthy of a kami's blessing."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b0a9cc0-bb6d-47f7-b0d5-3956c46a1b3a.jpg?1783923925"
    }
}
