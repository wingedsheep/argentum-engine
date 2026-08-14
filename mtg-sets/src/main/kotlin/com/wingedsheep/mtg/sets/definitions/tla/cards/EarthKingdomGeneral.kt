package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Earth Kingdom General
 * {3}{G}
 * Creature — Human Soldier Ally
 * 2/2
 *
 * When this creature enters, earthbend 2. (Target land you control becomes a 0/0 creature with
 * haste that's still a land. Put two +1/+1 counters on it. When it dies or is exiled, return it
 * to the battlefield tapped.)
 * Whenever you put one or more +1/+1 counters on a creature, you may gain that much life. Do this
 * only once each turn.
 *
 * Ability 1 is the standard ETB earthbend (`Effects.Earthbend(2, land)` over a targeted land you
 * control), mirroring Badgermole.
 *
 * Ability 2 mirrors Terrasymbiosis ("you put one or more +1/+1 counters on a creature you control,
 * you may draw that many cards. Do this only once each turn.") but pays off with life and is not
 * restricted to creatures you control — the oracle reads "a creature" (any creature). It fires on a
 * `Triggers.countersPlacedOn` for `Counters.PLUS_ONE_PLUS_ONE` over any creature with
 * `placedBy = Player.You` — the recipient filter is unrestricted, so the "you put" scope comes from
 * the placer selector (CR 122.6a), not from a "you control" recipient filter; a counter placed by
 * an opponent doesn't fire it. Gains `TRIGGER_COUNTERS_PLACED_AMOUNT` ("that much") life, wrapped in
 * `MayEffect` for the "may" (a bare `optional = true` is ignored on a no-target ability, like
 * Terrasymbiosis) plus `effectOncePerTurn = true` for "Do this only once each turn" — CR 603.2h,
 * the rider keyed to the action rather than the trigger cap.
 */
val EarthKingdomGeneral = card("Earth Kingdom General") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Soldier Ally"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, earthbend 2. (Target land you control becomes a 0/0 " +
        "creature with haste that's still a land. Put two +1/+1 counters on it. When it dies or " +
        "is exiled, return it to the battlefield tapped.)\n" +
        "Whenever you put one or more +1/+1 counters on a creature, you may gain that much life. " +
        "Do this only once each turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val land = target("target land you control", TargetObject(filter = TargetFilter.Land.youControl()))
        effect = Effects.Earthbend(2, land)
        description = "When this creature enters, earthbend 2."
    }

    triggeredAbility {
        trigger = Triggers.countersPlacedOn(
            filter = GameObjectFilter.Creature,
            counterType = Counters.PLUS_ONE_PLUS_ONE,
            firstTimeEachTurn = false,
            binding = TriggerBinding.ANY,
            placedBy = Player.You,
        )
        // CR 603.2h — keyed to the life gain, not to the trigger: "Once you choose to gain life
        // using Earth Kingdom General's second ability, that ability won't trigger again that
        // turn" (Scryfall ruling). Declining a small placement keeps a bigger one later live.
        effectOncePerTurn = true
        // The "you may" is a MayEffect, not a bare `optional = true` — the engine ignores that
        // flag on a no-target ability with no elseEffect (same as Terrasymbiosis), gaining the life
        // unconditionally instead of offering the choice.
        effect = MayEffect(
            Effects.GainLife(
                DynamicAmount.ContextProperty(ContextPropertyKey.TRIGGER_COUNTERS_PLACED_AMOUNT)
            )
        )
        description = "Whenever you put one or more +1/+1 counters on a creature, you may gain " +
            "that much life. Do this only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "173"
        artist = "Alexandr Leskinen"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/8688fce5-74b1-41e1-a59a-05a3878a75cb.jpg?1764121177"
    }
}
