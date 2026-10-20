package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Venarian Gold — Legends #83
 * {X}{U}{U} · Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, tap enchanted creature and put X sleep counters on it.
 * Enchanted creature doesn't untap during its controller's untap step if it has a sleep counter on it.
 * At the beginning of the upkeep of enchanted creature's controller, remove a sleep counter from that
 * creature.
 *
 * Paralyze's shape with the sleep counters as the timer. X is read as [DynamicAmounts.castX] — the
 * spell is gone by the time the enters trigger resolves, so the X has to come off the permanent's
 * durable cast choices. The untap lock is the counter-gated half of Glyph of Delusion, but the
 * counters sit on the *creature*, not the Aura, so the gate is in the filter: the attached permanent
 * only while it carries a sleep counter (`Scope.AttachedTo` can't carry a narrowing filter, hence
 * `attachedToBySource()`, as Merseine does). The upkeep trigger is the Aura's own, timed off the
 * enchanted creature's controller (Cursed Land's `Triggers.attached`).
 */
val VenarianGold = card("Venarian Gold") {
    manaCost = "{X}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, tap enchanted creature and put X sleep counters on it.\n" +
        "Enchanted creature doesn't untap during its controller's untap step if it has a sleep counter on it.\n" +
        "At the beginning of the upkeep of enchanted creature's controller, remove a sleep counter from that creature."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Tap(EffectTarget.EnchantedCreature) then
            Effects.AddDynamicCounters(CounterType.SLEEP, DynamicAmounts.castX(), EffectTarget.EnchantedCreature)
        description = "When this Aura enters, tap enchanted creature and put X sleep counters on it."
    }

    staticAbility {
        ability = GrantKeyword(
            AbilityFlag.DOESNT_UNTAP.name,
            filter = GroupFilter(GameObjectFilter.Any.attachedToBySource().withCounter(CounterType.SLEEP))
        )
    }

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.RemoveCounters(CounterType.SLEEP, 1, EffectTarget.EnchantedCreature)
        description = "At the beginning of the upkeep of enchanted creature's controller, remove a sleep " +
            "counter from that creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83"
        artist = "Daniel Gelon"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11fb92c0-bb1e-463a-a6b6-887a5d0cb873.jpg?1783948070"
        ruling(
            "2011-06-01",
            "The counters are placed on the creature to which it enters attached. If you later move Venarian " +
                "Gold to a different creature, the counters remain on the original creature and no counters " +
                "are put on the new creature."
        )
    }
}
