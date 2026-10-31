package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mace of the Valiant
 * {2}{W}
 * Artifact — Equipment
 * Equipped creature gets +1/+1 for each charge counter on this Equipment and has vigilance.
 * Whenever a creature you control enters, put a charge counter on this Equipment.
 * Equip {3}
 *
 * Banshee's Blade's counter-scaling static ([GrantDynamicStats] over the Equipment's own charge
 * counters) plus a vigilance grant, fed by Cathars' Crusade's "a creature you control enters"
 * trigger with the counter landing on the Equipment itself.
 */
val MaceOfTheValiant = card("Mace of the Valiant") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1 for each charge counter on this Equipment and has vigilance. " +
        "(Attacking doesn't cause it to tap.)\n" +
        "Whenever a creature you control enters, put a charge counter on this Equipment.\n" +
        "Equip {3}"

    staticAbility {
        val chargeCounters = DynamicAmounts.countersOnSelf(CounterType.CHARGE)
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = chargeCounters,
            toughnessBonus = chargeCounters,
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.AddCounters(CounterType.CHARGE, 1, EffectTarget.Self)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "314"
        artist = "Aaron Miller"
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6c141ed6-02ee-49c4-98af-938ebcefb2f5.jpg?1783932553"
    }
}
