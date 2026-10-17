package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lion Sash — Kamigawa: Neon Dynasty #26 (canonical printing)
 * {1}{W} · Artifact Creature — Equipment Cat · 1/1
 *
 * {W}: Exile target card from a graveyard. If it was a permanent card, put a +1/+1 counter on
 * this permanent.
 * Equipped creature gets +1/+1 for each +1/+1 counter on this Equipment.
 * Reconfigure {2}
 *
 * "If it was a permanent card" reads the exiled card's printed types, so it holds in exile.
 */
val LionSash = card("Lion Sash") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Equipment Cat"
    power = 1
    toughness = 1
    oracleText = "{W}: Exile target card from a graveyard. If it was a permanent card, put a +1/+1 " +
        "counter on this permanent.\n" +
        "Equipped creature gets +1/+1 for each +1/+1 counter on this Equipment.\n" +
        "Reconfigure {2} ({2}: Attach to target creature you control; or unattach from a creature. " +
        "Reconfigure only as a sorcery. While attached, this isn't a creature.)"

    activatedAbility {
        cost = Costs.Mana(ManaCost.parse("{W}"))
        val exiled = target(TargetFilter.CardInGraveyard)
        effect = Effects.Exile(exiled) then
            Effects.If(
                condition = Conditions.TargetMatchesFilter(GameObjectFilter.Permanent, 0),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            )
        description = "{W}: Exile target card from a graveyard. If it was a permanent card, " +
            "put a +1/+1 counter on this permanent."
    }

    val counters = DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE)
    staticAbility {
        ability = GrantDynamicStats(
            filter = Filters.EquippedCreature,
            powerBonus = counters,
            toughnessBonus = counters,
        )
    }

    reconfigure("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e1766e9-2fa7-4446-a255-7beea1467ece.jpg?1783923917"
    }
}
