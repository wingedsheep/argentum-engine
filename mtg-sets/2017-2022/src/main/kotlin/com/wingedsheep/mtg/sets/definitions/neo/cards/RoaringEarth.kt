package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Roaring Earth — Kamigawa: Neon Dynasty #204 (canonical printing)
 * {1}{G} Enchantment · Uncommon
 *
 * Landfall — Whenever a land you control enters, put a +1/+1 counter on target creature or
 * Vehicle you control.
 * Channel — {X}{G}{G}, Discard this card: Put X +1/+1 counters on target land you control. It
 * becomes a 0/0 green Spirit creature with haste. It's still a land.
 *
 * Landfall is the Canopy Baloth trigger shape; the counter target is
 * [GameObjectFilter.CreatureOrVehicle] (Seven-Tail Mentor), so an uncrewed Vehicle is legal.
 *
 * Channel is the Otawara shape (activated from hand, [Costs.DiscardSelf]) with an {X} in the mana
 * cost, read back by [DynamicAmounts.xValue] (Blast Zone). The animation is Tendril of the
 * Mycotyrant's permanent [Effects.BecomeCreature]: base 0/0, Spirit, green, haste, no duration —
 * BecomeCreature adds the creature type without stripping the land type. With X = 0 the land is
 * a 0/0 and dies to state-based actions, as the card intends.
 */
val RoaringEarth = card("Roaring Earth") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Landfall — Whenever a land you control enters, put a +1/+1 counter on target creature or " +
        "Vehicle you control.\n" +
        "Channel — {X}{G}{G}, Discard this card: Put X +1/+1 counters on target land you control. " +
        "It becomes a 0/0 green Spirit creature with haste. It's still a land."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val t = target(TargetFilter(GameObjectFilter.CreatureOrVehicle.youControl()))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
        description = "Landfall — Whenever a land you control enters, put a +1/+1 counter on target " +
            "creature or Vehicle you control."
    }

    // Channel — {X}{G}{G}, Discard this card (from hand): animate a land you control permanently.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{G}{G}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.xValue(),
            target = land,
        ) then Effects.BecomeCreature(
            target = land,
            power = 0,
            toughness = 0,
            keywords = setOf(Keyword.HASTE),
            creatureTypes = setOf("Spirit"),
            colors = setOf("GREEN"),
            duration = Duration.Permanent,
        )
        description = "Channel — {X}{G}{G}, Discard this card: Put X +1/+1 counters on target land you " +
            "control. It becomes a 0/0 green Spirit creature with haste. It's still a land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "204"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cbdd88e-c532-4fc0-a1fe-8aa0c26d1092.jpg?1783923842"
    }
}
