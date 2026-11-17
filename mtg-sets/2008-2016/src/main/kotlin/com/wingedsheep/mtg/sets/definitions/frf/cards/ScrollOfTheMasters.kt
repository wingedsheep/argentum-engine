package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scroll of the Masters
 * {2}
 * Artifact
 * Whenever you cast a noncreature spell, put a lore counter on this artifact.
 * {3}, {T}: Target creature you control gets +1/+1 until end of turn for each lore counter on this
 * artifact.
 *
 * The lore count is read once as the ability resolves and frozen into the pump (ruling). If the
 * Scroll has left the battlefield by then, [DynamicAmounts.countersOnSelf] falls back to the
 * counters it last had on the battlefield (last-known information).
 */
val ScrollOfTheMasters = card("Scroll of the Masters") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Whenever you cast a noncreature spell, put a lore counter on this artifact.\n" +
        "{3}, {T}: Target creature you control gets +1/+1 until end of turn for each lore counter on " +
        "this artifact."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.LORE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val creature = target(TargetFilter.CreatureYouControl)
        val lore = DynamicAmounts.countersOnSelf(CounterType.LORE)
        effect = Effects.ModifyStats(lore, lore, creature)
        description = "{3}, {T}: Target creature you control gets +1/+1 until end of turn for each " +
            "lore counter on this artifact."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6c7f24e-5ead-4b32-a804-4c89929508f6.jpg?1783938675"
        ruling(
            "2014-11-24",
            "Count the number of lore counters on Scroll of the Masters as the activated ability " +
                "resolves to determine how big a bonus the target creature gets. If Scroll of the " +
                "Masters isn't on the battlefield at that time, use the number of lore counters on it " +
                "when it left the battlefield."
        )
        ruling(
            "2014-11-24",
            "Once the activated ability has resolved, changing the number of lore counters on Scroll " +
                "of the Masters won't affect the bonus that activation granted."
        )
    }
}
