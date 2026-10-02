package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glyph Elemental — Modern Horizons 3 #27
 *
 * The landfall counter always lands on the permanent itself. As a creature those counters pump it
 * directly; bestowed, [GrantDynamicStats] over [GroupFilter.attachedCreature] reads them off the
 * Aura ([DynamicAmounts.countersOnSelf]) and hands the bonus to the enchanted creature. Unattached,
 * the filter matches nothing, so the bonus never double-counts.
 */
val GlyphElemental = card("Glyph Elemental") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Elemental"
    power = 2
    toughness = 2
    oracleText = "Bestow {1}{W} (If you cast this card for its bestow cost, it's an Aura spell with enchant creature. It becomes a creature again if it's not attached.)\n" +
        "Landfall — Whenever a land you control enters, put a +1/+1 counter on this permanent.\n" +
        "Enchanted creature gets +1/+1 for each +1/+1 counter on this Aura."

    keywordAbility(KeywordAbility.bestow("{1}{W}"))

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    staticAbility {
        val plusOneCounters = DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE)
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = plusOneCounters,
            toughnessBonus = plusOneCounters,
        )
    }

    metadata {
        ruling("2024-06-07", "Unlike other Auras, an Aura with bestow isn't put into its owner's graveyard if it becomes unattached. Rather, the effect making it an Aura ends, it loses enchant creature, and it remains on the battlefield as an enchantment creature. It can attack (and its {T} abilities can be activated, if it has any) on the turn it becomes unattached if it's been under your control continuously, even as an Aura, since your most recent turn began.")
        ruling("2024-06-07", "Unlike other Aura spells, an Aura spell with bestow isn't countered if its target is illegal as it begins to resolve. Rather, the effect making it an Aura spell ends, it loses enchant creature, it returns to being an enchantment creature spell, and it resolves and enters the battlefield as an enchantment creature.")
        ruling("2024-06-07", "If a permanent with bestow enters the battlefield by any method other than being cast, it will be an enchantment creature. You can't choose to pay the bestow cost and have it become an Aura.")
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Domenico Cava"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89f48c8f-b6f9-43b2-91c3-8d6e95d62db9.jpg?1783911301"
    }
}
