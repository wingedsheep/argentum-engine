package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emiel the Blessed {2}{W}{W}
 * Legendary Creature — Unicorn
 * 4/4
 *
 * {3}: Exile another target creature you control, then return it to the battlefield under its
 * owner's control.
 * Whenever another creature you control enters, you may pay {G/W}. If you do, put a +1/+1 counter
 * on it. If it's a Unicorn, put two +1/+1 counters on it instead.
 *
 * The Unicorn check reads the entering creature as it is when the trigger resolves; paying is
 * required for either branch (ruling).
 */
val EmielTheBlessed = card("Emiel the Blessed") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "WG"
    typeLine = "Legendary Creature — Unicorn"
    oracleText = "{3}: Exile another target creature you control, then return it to the battlefield under its owner's control.\n" +
        "Whenever another creature you control enters, you may pay {G/W}. If you do, put a +1/+1 counter on it. " +
        "If it's a Unicorn, put two +1/+1 counters on it instead. ({G/W} can be paid with either {G} or {W}.)"
    power = 4
    toughness = 4

    activatedAbility {
        cost = Costs.Mana("{3}")
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.Move(creature, Zone.EXILE) then
            Effects.Move(creature, Zone.BATTLEFIELD)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.MayPay(
            cost = ManaCost.parse("{G/W}"),
            then = Effects.If(
                condition = Conditions.EntityMatches(
                    EffectTarget.TriggeringEntity,
                    GameObjectFilter.Any.withSubtype("Unicorn"),
                ),
                then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.TriggeringEntity),
                otherwise = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity),
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f74dfd07-d17c-4890-82c3-4b12a6029940.jpg?1783930510"

        ruling("2020-06-23", "When the card returns to the battlefield, it will be a new object with no connection to the card that was exiled. Auras attached to the exiled creature will be put into their owners' graveyards. Any Equipment will become unattached and remain on the battlefield. Any counters on the exiled creature will cease to exist.")
        ruling("2020-06-23", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2020-06-23", "Emiel's second ability triggers whenever any creature other than itself enters the battlefield under your control, including those returned by its first ability.")
        ruling("2020-06-23", "You choose whether to pay for Emiel's triggered ability while it's resolving. If you do, no player may take other actions between the time you pay and the time the creature has one or two +1/+1 counters on it.")
        ruling("2020-06-23", "While resolving Emiel's triggered ability, you can't pay {G/W} more than once to put more counters on the creature.")
        ruling("2020-06-23", "If the entering creature is a Unicorn, you still have to pay {G/W} to put two +1/+1 counters on it.")
    }
}
