package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Shantotto, Tactician Magician
 * {1}{U}{R}
 * Legendary Creature — Dwarf Wizard
 * 0/4
 * Whenever you cast a noncreature spell, Shantotto gets +X/+0 until end of turn, where X is the
 * amount of mana spent to cast that spell. If X is 4 or more, draw a card.
 *
 * X reads `DynamicAmount.ContextProperty(MANA_SPENT_ON_TRIGGERING_SPELL)` — the mana actually spent
 * on the *triggering* spell (the same primitive [com.wingedsheep.mtg.sets.definitions.sos.cards.AberrantManawurm]
 * uses), distinct from `TRIGGERING_SPELL_MANA_VALUE` (printed mana value). The +X/+0 lasts until end
 * of turn (the `ModifyStats` default duration). The conditional draw compares that same mana-spent
 * amount against 4, so cost reductions / extra payments are honored consistently across both halves.
 */
val ShantottoTacticianMagician = card("Shantotto, Tactician Magician") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Dwarf Wizard"
    power = 0
    toughness = 4
    oracleText = "Whenever you cast a noncreature spell, Shantotto gets +X/+0 until end of turn, " +
        "where X is the amount of mana spent to cast that spell. If X is 4 or more, draw a card."

    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = Effects.ModifyStats(
            power = DynamicAmount.ContextProperty(ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL),
            toughness = DynamicAmount.Fixed(0),
            target = EffectTarget.Self,
        ) then ConditionalEffect(
            condition = Conditions.CompareAmounts(
                left = DynamicAmount.ContextProperty(ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL),
                operator = ComparisonOperator.GTE,
                right = DynamicAmount.Fixed(4),
            ),
            effect = Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "241"
        artist = "Joshua Raphael"
        flavorText = "\"Ohohoho! I might be the harshest mistress in all of Vana'diel. But I may yet " +
            "forgive you, if at my feet you kneel.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/f/eff984b2-6ea9-4471-91c5-99c47f87f10b.jpg?1748706682"
    }
}
