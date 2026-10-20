package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Cocoon — Legends #178
 * {G} · Enchantment — Aura
 *
 * Enchant creature you control
 * When this Aura enters, tap enchanted creature and put three pupa counters on this Aura.
 * Enchanted creature doesn't untap during your untap step if this Aura has a pupa counter on it.
 * At the beginning of your upkeep, remove a pupa counter from this Aura. If you can't, sacrifice it,
 * put a +1/+1 counter on enchanted creature, and that creature gains flying.
 *
 * The pupa counters sit on the Aura (unlike Venarian Gold's sleep counters), so the untap lock is a
 * [ConditionalStaticAbility] gated on the source's own counters. The hatch runs in printed order:
 * once the Aura is sacrificed, "enchanted creature" reads the host off the Aura's last-known
 * battlefield snapshot (CR 608.2h), and the flying it grants is permanent — it belongs to the
 * resolved ability, not to the departed Aura.
 */
val Cocoon = card("Cocoon") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "When this Aura enters, tap enchanted creature and put three pupa counters on this Aura.\n" +
        "Enchanted creature doesn't untap during your untap step if this Aura has a pupa counter on it.\n" +
        "At the beginning of your upkeep, remove a pupa counter from this Aura. If you can't, sacrifice it, " +
        "put a +1/+1 counter on enchanted creature, and that creature gains flying."
    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature) then
            Effects.AddCounters(CounterType.PUPA, 3, EffectTarget.Self)
        description = "When this Aura enters, tap enchanted creature and put three pupa counters on this Aura."
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(
                AbilityFlag.DOESNT_UNTAP.name,
                filter = GroupFilter(GameObjectFilter.Any.attachedToBySource())
            ),
            condition = Conditions.SourceHasCounter(CounterType.PUPA),
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.SourceHasCounter(CounterType.PUPA),
            then = Effects.RemoveCounters(CounterType.PUPA, 1, EffectTarget.Self),
            otherwise = Effects.SacrificeTarget(EffectTarget.Self) then
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.EnchantedCreature) then
                Effects.GrantKeyword(Keyword.FLYING, EffectTarget.EnchantedCreature, Duration.Permanent)
        )
        description = "At the beginning of your upkeep, remove a pupa counter from this Aura. If you can't, " +
            "sacrifice it, put a +1/+1 counter on enchanted creature, and that creature gains flying."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a82c87b1-de37-4423-a1a4-533a1d8108b2.jpg?1783948049"
        ruling("2005-08-01", "If the Aura is moved to a different creature, the number of counters is unchanged.")
        ruling("2004-10-04", "The creature can still be untapped using spells and abilities.")
        ruling("2004-10-04", "Cocoon can be used on a tapped creature.")
    }
}
