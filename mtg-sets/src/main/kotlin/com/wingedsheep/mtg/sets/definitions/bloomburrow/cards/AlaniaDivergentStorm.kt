package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.AnyCondition
import com.wingedsheep.sdk.scripting.conditions.IsFirstSpellOfTypeCastThisTurn
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Alania, Divergent Storm
 * {3}{U}{R}
 * Legendary Creature — Otter Wizard
 * 3/5
 * Whenever you cast a spell, if it's the first instant spell, the first sorcery spell,
 * or the first Otter spell other than Alania you've cast this turn, you may have target
 * opponent draw a card. If you do, copy that spell. You may choose new targets for the copy.
 */
val AlaniaDivergentStorm = card("Alania, Divergent Storm") {
    manaCost = "{3}{U}{R}"
    typeLine = "Legendary Creature — Otter Wizard"
    oracleText = "Whenever you cast a spell, if it's the first instant spell, the first sorcery spell, or the first Otter spell other than Alania you've cast this turn, you may have target opponent draw a card. If you do, copy that spell. You may choose new targets for the copy."
    power = 3
    toughness = 5

    triggeredAbility {
        trigger = Triggers.YouCastSpell
        triggerCondition = AnyCondition(
            listOf(
                IsFirstSpellOfTypeCastThisTurn(GameObjectFilter.Instant),
                IsFirstSpellOfTypeCastThisTurn(GameObjectFilter.Sorcery),
                IsFirstSpellOfTypeCastThisTurn(GameObjectFilter.Any.withSubtype(com.wingedsheep.sdk.core.Subtype("Otter")))
            )
        )
        val opponent = target("opponent", Targets.Opponent)
        effect = ReflexiveTriggerEffect(
            action = DrawCardsEffect(DynamicAmount.Fixed(1), opponent),
            optional = true,
            reflexiveEffect = CopyTargetSpellEffect(EffectTarget.TriggeringEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Joshua Raphael"
        flavorText = "\"Why would I chase the storm? Does a cloud chase the rain? Does the sun chase the light?\""
        imageUri = "https://cards.scryfall.io/normal/front/4/3/436d6a84-4cea-4ca7-94aa-9d08280652af.jpg?1721426997"
        ruling("2024-07-26", "Alania's ability can trigger up to three times in a single turn: once when you cast your first instant, once when you cast your first sorcery, and once when you cast your first Otter spell other than Alania.")
        ruling("2024-07-26", "Alania's ability and the copy it creates both resolve before the spell that caused the ability to trigger. They resolve even if the spell is countered before the copy is created.")
        ruling("2024-07-26", "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell won't trigger.")
    }
}
