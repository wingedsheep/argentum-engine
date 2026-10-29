package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Malefic Scythe
 * {1}{B}
 * Artifact — Equipment
 * This Equipment enters with a soul counter on it.
 * Equipped creature gets +1/+1 for each soul counter on this Equipment.
 * Whenever equipped creature dies, put a soul counter on this Equipment.
 * Equip {1}
 *
 * Banshee's Blade's counter-scaling static ([GrantDynamicStats] reading [DynamicAmounts.countersOnSelf]
 * off the Equipment) fed by Deathrender's `Triggers.attached.dies()` — the Scythe is still attached
 * when its host dies, and the counter lands on the Equipment itself.
 */
val MaleficScythe = card("Malefic Scythe") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Artifact — Equipment"
    oracleText = "This Equipment enters with a soul counter on it.\n" +
        "Equipped creature gets +1/+1 for each soul counter on this Equipment.\n" +
        "Whenever equipped creature dies, put a soul counter on this Equipment.\n" +
        "Equip {1}"

    replacementEffect(EntersWithCounters(counterType = CounterType.SOUL, count = 1, selfOnly = true))

    staticAbility {
        val soulCounters = DynamicAmounts.countersOnSelf(CounterType.SOUL)
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = soulCounters,
            toughnessBonus = soulCounters,
        )
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.AddCounters(CounterType.SOUL, 1, EffectTarget.Self)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Joe Slucher"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89e2bc57-8f18-4ba1-a11b-9d69d029f56a.jpg?1783930705"
    }
}
