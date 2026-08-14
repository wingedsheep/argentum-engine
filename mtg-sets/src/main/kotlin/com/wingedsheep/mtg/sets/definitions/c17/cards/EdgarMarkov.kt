package com.wingedsheep.mtg.sets.definitions.c17.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Edgar Markov — Commander 2017 #36 (earliest printing; later reprinted in Innistrad Remastered).
 *
 * {3}{R}{W}{B} Legendary Creature — Vampire Knight, 4/4
 *   Eminence — Whenever you cast another Vampire spell, if Edgar is in the command zone or on the
 *   battlefield, create a 1/1 black Vampire creature token.
 *   First strike, haste
 *   Whenever Edgar attacks, put a +1/+1 counter on each Vampire you control.
 *
 * **Eminence** is an ability word (CR 207.2c) — no rules meaning of its own. What makes the ability
 * work is CR 113.6b: "an ability that states which zones it functions in functions only from those
 * zones". The printed "if Edgar is in the command zone or on the battlefield" is that statement, so
 * the trigger condition functions from both zones, which is exactly
 * `triggerZones = setOf(Zone.BATTLEFIELD, Zone.COMMAND)`. `TriggerDetector` scans the command zone
 * for those (see `NON_BATTLEFIELD_EVENT_TRIGGER_ZONES`); no eminence-specific engine type exists.
 *
 * The same clause is also an intervening-"if" (CR 603.4), so it is checked *twice* — when the
 * trigger fires and again as the ability resolves. The official ruling is explicit: "If it's on the
 * battlefield or in the command zone when you cast another Vampire spell but leaves that zone before
 * the ability resolves, the ability won't do anything as it resolves." `triggerZones` covers the
 * fire-time half; the [ConditionalEffect] on [Conditions.SourceInZone] covers the resolution-time
 * half, so killing Edgar in response to the Vampire spell produces no token.
 *
 * **"another"** needs no filter of its own. For the triggering spell to *be* Edgar, Edgar's card has
 * to be on the stack — and a card on the stack is in neither of the two zones this ability functions
 * from, so the zone gate already declines it. Casting Edgar out of the command zone therefore makes
 * no token, which `EdgarMarkovScenarioTest` pins down.
 *
 * The attack trigger reuses the Cathars' Crusade idiom: [Effects.ForEachInGroup] over the Vampires
 * you control, with the inner counter aimed at [EffectTarget.Self] (the iterated member).
 */
val EdgarMarkov = card("Edgar Markov") {
    manaCost = "{3}{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Legendary Creature — Vampire Knight"
    power = 4
    toughness = 4
    oracleText = "Eminence — Whenever you cast another Vampire spell, if Edgar is in the command " +
        "zone or on the battlefield, create a 1/1 black Vampire creature token.\n" +
        "First strike, haste\n" +
        "Whenever Edgar attacks, put a +1/+1 counter on each Vampire you control."

    keywords(Keyword.FIRST_STRIKE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.YouCastSubtype(Subtype.VAMPIRE)
        triggerZones = setOf(Zone.BATTLEFIELD, Zone.COMMAND)
        effect = ConditionalEffect(
            condition = Conditions.SourceInZone(Zone.BATTLEFIELD, Zone.COMMAND),
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Vampire"),
            ),
        )
        description = "Eminence — Whenever you cast another Vampire spell, if Edgar is in the " +
            "command zone or on the battlefield, create a 1/1 black Vampire creature token."
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl().withSubtype(Subtype.VAMPIRE)),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
        )
        description = "Whenever Edgar attacks, put a +1/+1 counter on each Vampire you control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "36"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d94b8ec-ecda-43c8-a60e-1ba33e6a54a4.jpg?1783935938"

        ruling("2017-08-25", "Edgar Markov's eminence ability is a triggered ability. Edgar must be on the battlefield or in the command zone when you cast another Vampire spell and also as the triggered ability resolves. If it's on the battlefield or in the command zone when you cast another Vampire spell but leaves that zone before the ability resolves, the ability won't do anything as it resolves.")
        ruling("2017-08-25", "Notably, if Edgar Markov is on the battlefield and its eminence ability triggers, but it's put into the command zone before that ability resolves, that ability won't do anything as it resolves. This is because an object that changes zones is considered a new object.")
    }
}
