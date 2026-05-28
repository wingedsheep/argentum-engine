package com.wingedsheep.mtg.sets.definitions.jud.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Soulcatchers' Aerie
 * {1}{W}
 * Enchantment
 * Whenever a Bird is put into your graveyard from the battlefield, put a feather
 * counter on this enchantment.
 * Bird creatures get +1/+1 for each feather counter on this enchantment.
 */
val SoulcatchersAerie = card("Soulcatchers' Aerie") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Whenever a Bird is put into your graveyard from the battlefield, put a feather counter on this enchantment.\nBird creatures get +1/+1 for each feather counter on this enchantment."

    // Whenever a Bird is put into your graveyard from the battlefield, accrue a feather counter.
    // "Your graveyard" is ownership, not control: a permanent always goes to its owner's
    // graveyard (CR 400.3), so we filter by ownedByYou rather than youControl.
    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Creature.withSubtype("Bird").ownedByYou(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = Effects.AddCounters(Counters.FEATHER, 1, EffectTarget.Self)
    }

    // Bird creatures (any controller) get +1/+1 for each feather counter on this enchantment.
    staticAbility {
        val featherCount = DynamicAmounts.countersOnSelf(CounterTypeFilter.Named(Counters.FEATHER))
        ability = GrantDynamicStatsEffect(
            filter = GroupFilter.allCreaturesWithSubtype("Bird"),
            powerBonus = featherCount,
            toughnessBonus = featherCount
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "25"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b30df994-bb09-4d16-8443-223c6ce342dc.jpg?1562631545"
    }
}
