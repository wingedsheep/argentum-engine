package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Tourach's Gate
 * {1}{B}{B}
 * Enchantment — Aura
 * Enchant land you control
 * Sacrifice a Thrull: Put three time counters on this Aura.
 * At the beginning of your upkeep, remove a time counter from this Aura. If there are no time
 * counters on this Aura, sacrifice it.
 * Tap enchanted land: Attacking creatures you control get +2/-1 until end of turn. Activate only
 * if enchanted land is untapped.
 *
 * The Aura enters with no counters, so it dies on the very next upkeep unless a Thrull is fed to
 * it — the sacrifice is what buys turns, three at a time. "Activate only if enchanted land is
 * untapped" needs no separate restriction: the ability's only cost is tapping that land, and a
 * tapped land can't pay it.
 */
val TourachsGate = card("Tourach's Gate") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land you control\n" +
        "Sacrifice a Thrull: Put three time counters on this Aura.\n" +
        "At the beginning of your upkeep, remove a time counter from this Aura. If there are no " +
        "time counters on this Aura, sacrifice it.\n" +
        "Tap enchanted land: Attacking creatures you control get +2/-1 until end of turn. " +
        "Activate only if enchanted land is untapped."
    auraTarget = TargetPermanent(filter = TargetFilter(GameObjectFilter.Land.youControl()))

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype(Subtype.THRULL))
        effect = Effects.AddCounters(Counters.TIME, 3, EffectTarget.Self)
        description = "Sacrifice a Thrull: Put three time counters on this Aura."
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = Effects.RemoveCounters(Counters.TIME, 1, EffectTarget.Self)
            .then(
                ConditionalEffect(
                    condition = Conditions.Not(Conditions.SourceCounterCountAtLeast(Counters.TIME, 1)),
                    effect = SacrificeSelfEffect
                )
            )
        description = "At the beginning of your upkeep, remove a time counter from this Aura. If there are no time counters on this Aura, sacrifice it."
    }

    activatedAbility {
        cost = Costs.TapAttachedCreature
        effect = Patterns.Group.modifyStatsForAll(
            power = 2,
            toughness = -1,
            filter = GroupFilter(GameObjectFilter.Creature.attacking().youControl())
        )
        description = "Tap enchanted land: Attacking creatures you control get +2/-1 until end of turn. " +
            "Activate only if enchanted land is untapped."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "48"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d77f6401-a9fb-449c-b511-6fb837055bb4.jpg?1783947898"
    }
}
