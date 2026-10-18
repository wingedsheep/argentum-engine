package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Selfless Samurai — Kamigawa: Neon Dynasty #35 (canonical printing)
 * {1}{W} · Creature — Fox Samurai · 2/2
 *
 * Whenever a Samurai or Warrior you control attacks alone, it gains lifelink until end of turn.
 * Sacrifice this creature: Another target creature you control gains indestructible until end of turn.
 *
 * "it" is the lone attacker bound by the ANY-bound `attacks` trigger → [EffectTarget.TriggeringEntity].
 */
val SelflessSamurai = card("Selfless Samurai") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox Samurai"
    power = 2
    toughness = 2
    oracleText = "Whenever a Samurai or Warrior you control attacks alone, it gains lifelink until end of turn.\n" +
        "Sacrifice this creature: Another target creature you control gains indestructible until end of turn. " +
        "(Damage and effects that say \"destroy\" don't destroy it.)"

    triggeredAbility {
        trigger = Triggers.a(
            GameObjectFilter.Creature.withAnyOfSubtypes(listOf(Subtype.SAMURAI, Subtype.WARRIOR)).youControl()
        ).attacks(setOf(AttackPredicate.Alone))
        effect = Effects.GrantKeyword(Keyword.LIFELINK, EffectTarget.TriggeringEntity, Duration.EndOfTurn)
        description = "Whenever a Samurai or Warrior you control attacks alone, it gains lifelink until end of turn."
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature, Duration.EndOfTurn)
        description = "Sacrifice this creature: Another target creature you control gains indestructible until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "PINDURSKI"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/76c27ec5-5a9b-4080-9a53-00630335a915.jpg?1783923914"
    }
}
