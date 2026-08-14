package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Banshee's Blade — Mirrodin #144
 * {2} · Artifact — Equipment
 *
 * Equipped creature gets +1/+1 for each charge counter on this Equipment.
 * Whenever equipped creature deals combat damage, put a charge counter on this Equipment.
 * Equip {2}
 *
 * The Withering Hex shape, on an Equipment instead of an Aura: a [GrantDynamicStatsEffect] over
 * [GroupFilter.attachedCreature] whose bonus reads the charge counters off the *Equipment*
 * ([DynamicAmounts.countersOnSelf] — `EntityReference.Source` is the permanent bearing the static,
 * not the creature it buffs). It recomputes continuously, so the bonus tracks counters as they
 * accrue and follows the Blade when it moves.
 *
 * The counter trigger is [TriggerBinding.ATTACHED] over a combat [DamageType] with the default
 * `RecipientFilter.Any` — the printed text says "deals combat damage", full stop, so damage to a
 * blocking creature counts exactly as much as damage to a player. Counters live on the Equipment
 * and are untouched by unattaching or re-equipping (2004 ruling); nothing here reads the creature,
 * so that falls out for free.
 */
val BansheesBlade = card("Banshee's Blade") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 for each charge counter on this Equipment.\n" +
        "Whenever equipped creature deals combat damage, put a charge counter on this Equipment.\n" +
        "Equip {2}"

    staticAbility {
        val chargeCounters = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.CHARGE))
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.attachedCreature(),
            powerBonus = chargeCounters,
            toughnessBonus = chargeCounters,
        )
    }

    triggeredAbility {
        trigger = Triggers.dealsDamage(
            damageType = DamageType.Combat,
            binding = TriggerBinding.ATTACHED,
        )
        effect = AddCountersEffect(Counters.CHARGE, 1, EffectTarget.Self)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Bradley Williams"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b77baad4-2bd6-492a-86ca-8e0088b751d2.jpg?1783944528"
        ruling(
            "2004-12-01",
            "The counters stay on Banshee's Blade even if it becomes unattached, or moves from one " +
                "creature to another."
        )
    }
}
