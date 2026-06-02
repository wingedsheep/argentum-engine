package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Synchronized Charge — Tarkir: Dragonstorm #162
 * {1}{G} · Sorcery
 *
 * Distribute two +1/+1 counters among one or two target creatures you control.
 * Creatures you control with counters on them gain vigilance and trample until end of
 * turn.
 * Harmonize {4}{G} (You may cast this card from your graveyard for its harmonize cost.
 * You may tap a creature you control to reduce that cost by {X}, where X is its power.
 * Then exile this spell.)
 *
 * The keyword-grant clause runs after the counters are distributed, so the freshly
 * buffed creatures (and any other creatures you control already carrying counters) are
 * included in the `withAnyCounter()` group at resolution time. Harmonize is the standard
 * graveyard alt-cost keyword (display + cast-from-graveyard support).
 */
val SynchronizedCharge = card("Synchronized Charge") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Distribute two +1/+1 counters among one or two target creatures you control. " +
        "Creatures you control with counters on them gain vigilance and trample until end of turn.\n" +
        "Harmonize {4}{G} (You may cast this card from your graveyard for its harmonize cost. " +
        "You may tap a creature you control to reduce that cost by {X}, where X is its power. Then exile this spell.)"

    spell {
        target("targets", TargetCreature(count = 2, minCount = 1, filter = com.wingedsheep.sdk.scripting.filters.unified.TargetFilter.CreatureYouControl))
        effect = Effects.Composite(listOf(
            Effects.DistributeCountersAmongTargets(totalCounters = 2),
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Creature.youControl().withAnyCounter()),
                effect = GrantKeywordEffect(Keyword.VIGILANCE, EffectTarget.Self)
            ),
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Creature.youControl().withAnyCounter()),
                effect = GrantKeywordEffect(Keyword.TRAMPLE, EffectTarget.Self)
            )
        ))
    }

    keywordAbility(KeywordAbility.harmonize("{4}{G}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "162"
        artist = "Johan Grenier"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f721f8d-fd2f-480b-8645-4bf6ce38dde9.jpg?1743204617"
        ruling("2025-04-04", "If two targets are chosen, you must choose to give each of them a +1/+1 counter. If only one of those two creatures is still a legal target at the time the spell resolves, it will receive only one counter.")
    }
}
