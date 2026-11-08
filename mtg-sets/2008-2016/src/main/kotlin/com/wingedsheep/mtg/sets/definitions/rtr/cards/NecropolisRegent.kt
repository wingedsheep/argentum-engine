package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Necropolis Regent
 * {3}{B}{B}{B}
 * Creature — Vampire
 * 6/5
 *
 * Flying
 * Whenever a creature you control deals combat damage to a player, put that many +1/+1 counters on it.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * The trigger is battlefield-wide over `GameObjectFilter.Creature.youControl()` (Old Gnawbone's
 * player-damage shape); "that many" reads the damage off the trigger payload and "it" is the damage
 * source (`EffectTarget.TriggeringEntity`) — the same composition as Quilled Greatwurm. A creature
 * that dies to simultaneous combat damage is gone before the trigger resolves and gets nothing.
 */
val NecropolisRegent = card("Necropolis Regent") {
    manaCost = "{3}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire"
    power = 6
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever a creature you control deals combat damage to a player, put that many +1/+1 counters on it."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.AddDynamicCounters(
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.TriggeringEntity,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "71"
        artist = "Winona Nelson"
        flavorText = "\"Jarad fancies himself king of the undercity, but he's merely king of rot.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b421dcc9-0299-416d-86bc-c70ef49bcf98.jpg?1783940361"
        ruling(
            "2012-10-01",
            "If that creature deals combat damage to a player at the same time it's dealt lethal damage " +
                "(perhaps because it has trample and was blocked), it will die before the triggered ability " +
                "resolves and puts +1/+1 counters on it."
        )
    }
}
