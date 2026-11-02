package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalLifeGainTriggers
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dr. Beverly Crusher
 * {2}{W}
 * Legendary Creature — Human Doctor
 * 2/2
 *
 * If you gaining life causes a triggered ability of a permanent you control to trigger, that
 * ability triggers an additional time.
 * Whenever you gain life, put a +1/+1 counter on Dr. Crusher.
 *
 * Her own life-gain trigger is a permanent you control's, so it is doubled too: each life-gain
 * event gives her two counters.
 */
val DrBeverlyCrusher = card("Dr. Beverly Crusher") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Creature — Human Doctor"
    colorIdentity = "W"
    oracleText = "If you gaining life causes a triggered ability of a permanent you control to trigger, " +
        "that ability triggers an additional time.\n" +
        "Whenever you gain life, put a +1/+1 counter on Dr. Crusher."
    power = 2
    toughness = 2

    staticAbility {
        ability = AdditionalLifeGainTriggers
    }

    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "13"
        artist = "Mattia Rangoni"
        flavorText = "\"Damn. Where are the calluses we doctors are supposed to grow over our feelings?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef9c5790-d909-4271-90bd-5fa4816ddd6b.jpg?1784065540"
    }
}
