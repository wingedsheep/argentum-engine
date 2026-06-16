package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Prairie Dog
 * {1}{W}
 * Creature — Squirrel
 * 2/2
 * Lifelink
 * At the beginning of your end step, if you haven't cast a spell from your hand this turn,
 * put a +1/+1 counter on this creature.
 * {4}{W}: Until end of turn, if you would put one or more +1/+1 counters on a creature you control,
 * put that many plus one +1/+1 counters on it instead.
 *
 * The end-step ability is an intervening-if (CR 603.4): "if you haven't cast a spell from your hand
 * this turn" is checked both when the ability would trigger and again as it resolves, modeled with
 * [triggerCondition] = `not(YouCastSpellsThisTurn(1, fromZone = HAND))` — the canonical idiom for
 * "you haven't cast a spell from your hand this turn".
 *
 * The {4}{W} activated ability installs a temporary, controller-scoped counter-placement modifier
 * (the activated analogue of Hardened Scales' static [com.wingedsheep.sdk.scripting.ModifyCounterPlacement])
 * via [Effects.GrantCounterPlacementModifier]: while active (until end of turn) it adds one extra
 * +1/+1 counter whenever the controller would put +1/+1 counters on a creature they control. It is
 * consulted from the single counter-placement chokepoint, so every counter-adding effect honors it,
 * and it expires at end of turn.
 */
val PrairieDog = card("Prairie Dog") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Squirrel"
    power = 2
    toughness = 2
    oracleText = "Lifelink\n" +
        "At the beginning of your end step, if you haven't cast a spell from your hand this turn, " +
        "put a +1/+1 counter on this creature.\n" +
        "{4}{W}: Until end of turn, if you would put one or more +1/+1 counters on a creature you control, " +
        "put that many plus one +1/+1 counters on it instead."

    keywords(Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.YourEndStep
        triggerCondition = Conditions.Not(Conditions.YouCastSpellsThisTurn(1, fromZone = Zone.HAND))
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "At the beginning of your end step, if you haven't cast a spell from your hand this turn, " +
            "put a +1/+1 counter on this creature."
    }

    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        effect = Effects.GrantCounterPlacementModifier()
        description = "Until end of turn, if you would put one or more +1/+1 counters on a creature you control, " +
            "put that many plus one +1/+1 counters on it instead."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "24"
        artist = "Kevin Sidharta"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37302b5d-e528-4baa-947a-c859e4ddcff9.jpg"
    }
}
