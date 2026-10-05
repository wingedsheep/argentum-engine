package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Anointer of Valor
 * {5}{W}
 * Creature — Angel
 * 3/5
 * Flying
 * Whenever a creature attacks, you may pay {3}. When you do, put a +1/+1 counter on that creature.
 *
 * "Whenever a creature attacks" is `Triggers.a().attacks()` — unfiltered, so it fires once per
 * attacking creature on either side of the table (CR 603.2c), Anointer itself included, and binds
 * the attacker as the triggering entity.
 *
 * "You may pay {3}. When you do, …" is a reflexive triggered ability (CR 603.12): the optional
 * {3} is paid while the attack trigger resolves, and only then does the counter ability go on the
 * stack as a separate object players may respond to. The reflexive half inherits the attack
 * trigger's record, so [EffectTarget.TriggeringEntity] still names "that creature". The payment
 * is one-shot — it can't be repeated for more counters.
 */
val AnointerOfValor = card("Anointer of Valor") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever a creature attacks, you may pay {3}. When you do, put a +1/+1 counter on that creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a().attacks()
        effect = Effects.ReflexiveTrigger(
            action = Effects.PayMana("{3}"),
            optional = true,
            reflexiveEffect = Effects.AddCounters(
                CounterType.PLUS_ONE_PLUS_ONE,
                1,
                EffectTarget.TriggeringEntity,
            ),
            descriptionOverride = "put a +1/+1 counter on that creature",
        )
        description = "Whenever a creature attacks, you may pay {3}. When you do, put a +1/+1 counter " +
            "on that creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Antonio José Manzanedo"
        flavorText = "Light streaked down from the heavens, and a voice whispered in the soldier's ear: " +
            "\"You shall not fall today.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d66fcbb8-0fa5-48a9-b40c-d5a12f09a858.jpg?1783928889"
        ruling(
            "2020-11-10",
            "Anointer of Valor's triggered ability triggers when any creature attacks any player or planeswalker."
        )
        ruling(
            "2020-11-10",
            "If you choose to pay {3}, a second triggered ability triggers. Players may respond to that " +
                "reflexive triggered ability before the +1/+1 counter is put on the creature."
        )
        ruling(
            "2020-11-10",
            "You can't pay {3} more than once to put more than one +1/+1 counter on the attacking creature."
        )
    }
}
