package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Temporal Distortion
 * {3}{U}{U}
 * Enchantment
 * Whenever a creature or land becomes tapped, put an hourglass counter on it.
 * Each permanent with an hourglass counter on it doesn't untap during its controller's untap step.
 * At the beginning of each player's upkeep, remove all hourglass counters from permanents that
 * player controls.
 *
 * The "doesn't untap" clause is modeled as a counter-keyed static ability that grants the
 * DOESNT_UNTAP flag to any permanent with an hourglass counter — reusing the untap step's
 * existing DOESNT_UNTAP handling and keeping the restriction projection-scoped (it disappears
 * if Temporal Distortion leaves the battlefield, per Rule 613/static-ability semantics). The
 * upkeep removal uses the active player (= the upkeep player) as the controller scope.
 */
val TemporalDistortion = card("Temporal Distortion") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature or land becomes tapped, put an hourglass counter on it.\n" +
        "Each permanent with an hourglass counter on it doesn't untap during its controller's " +
        "untap step.\n" +
        "At the beginning of each player's upkeep, remove all hourglass counters from permanents " +
        "that player controls."

    // Whenever a creature or land becomes tapped, put an hourglass counter on it.
    triggeredAbility {
        trigger = Triggers.becomesTapped(
            binding = TriggerBinding.ANY,
            filter = GameObjectFilter.CreatureOrLand
        )
        effect = Effects.AddCounters(Counters.HOURGLASS, 1, EffectTarget.TriggeringEntity)
    }

    // Each permanent with an hourglass counter on it doesn't untap during its controller's untap step.
    staticAbility {
        ability = GrantKeyword(
            AbilityFlag.DOESNT_UNTAP.name,
            GroupFilter(GameObjectFilter.Permanent.withCounter(Counters.HOURGLASS))
        )
    }

    // At the beginning of each player's upkeep, remove all hourglass counters from permanents
    // that player controls (the upkeep player is the active player).
    triggeredAbility {
        trigger = Triggers.EachUpkeep
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.controlledByActivePlayer()),
            RemoveCountersEffect(Counters.HOURGLASS, Int.MAX_VALUE, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "79"
        artist = "Stephanie Law"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/74bd0d14-8d26-403f-9405-d0dcdecd1a49.jpg?1562918372"
    }
}
