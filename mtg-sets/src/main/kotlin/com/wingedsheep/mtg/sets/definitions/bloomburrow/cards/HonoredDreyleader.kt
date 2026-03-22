package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Honored Dreyleader
 * {2}{G}
 * Creature — Squirrel Warrior
 * 1/1
 *
 * Trample
 * When this creature enters, put a +1/+1 counter on it for each other
 * Squirrel and/or Food you control.
 * Whenever another Squirrel or Food you control enters, put a +1/+1
 * counter on this creature.
 */
val HonoredDreyleader = card("Honored Dreyleader") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Squirrel Warrior"
    power = 1
    toughness = 1
    oracleText = "Trample\nWhen this creature enters, put a +1/+1 counter on it for each other Squirrel and/or Food you control.\nWhenever another Squirrel or Food you control enters, put a +1/+1 counter on this creature."

    keywords(Keyword.TRAMPLE)

    val squirrelOrFoodFilter = GameObjectFilter.Permanent.youControl()
        .withAnyOfSubtypes(listOf(Subtype("Squirrel"), Subtype("Food")))

    // ETB: +1/+1 counter for each other Squirrel and/or Food you control
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.AddDynamicCounters(
            "+1/+1",
            DynamicAmount.AggregateBattlefield(Player.You, squirrelOrFoodFilter, excludeSelf = true),
            EffectTarget.Self
        )
    }

    // Whenever another Squirrel or Food you control enters → +1/+1 counter
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = squirrelOrFoodFilter,
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.AddCounters("+1/+1", 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Aurore Folny"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc5ee537-52e1-474a-9326-dfacc2a758ab.jpg?1721426839"

        ruling("2024-07-26", "If a permanent you control is both a Squirrel and a Food, count it only once when determining how many counters to put on Honored Dreyleader.")
        ruling("2024-07-26", "If a permanent you control that is both a Squirrel and a Food enters, Honored Dreyleader's last ability will trigger only once.")
    }
}
