package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Aberrant Manawurm
 * {3}{G}
 * Creature — Wurm
 * 2/5
 * Trample
 * Whenever you cast an instant or sorcery spell, this creature gets +X/+0 until end of turn,
 * where X is the amount of mana spent to cast that spell.
 *
 * X reads `DynamicAmount.ContextProperty(MANA_SPENT_ON_TRIGGERING_SPELL)` — the mana spent on
 * the *triggering* spell (distinct from `DynamicAmount.TotalManaSpent`, which is the resolving
 * object's own cast). The +X/+0 lasts until end of turn (the `ModifyStats` default duration).
 */
val AberrantManawurm = card("Aberrant Manawurm") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 2
    toughness = 5
    oracleText = "Trample\nWhenever you cast an instant or sorcery spell, this creature gets " +
        "+X/+0 until end of turn, where X is the amount of mana spent to cast that spell."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.YouCastInstantOrSorcery
        effect = Effects.ModifyStats(
            power = DynamicAmount.ContextProperty(ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL),
            toughness = DynamicAmount.Fixed(0),
            target = EffectTarget.Self,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Lars Grant-West"
        flavorText = "\"What is the equation for terror?\"\n—Lost research note, Paradox Gardens"
        imageUri = "https://cards.scryfall.io/normal/front/7/9/797131cf-d80d-4050-bebd-2ce1d7fae5d0.jpg?1775937935"
    }
}
