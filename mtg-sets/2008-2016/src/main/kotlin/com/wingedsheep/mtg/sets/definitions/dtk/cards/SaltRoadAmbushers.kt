package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Salt Road Ambushers
 * {3}{G}
 * Creature — Dog Warrior
 * 3/3
 * Whenever another permanent you control is turned face up, if it's a creature, put two +1/+1
 * counters on it.
 * Megamorph {3}{G}{G}
 *
 * "Another" is `notSourceItself()` on the turned-face-up filter, so flipping this creature itself
 * doesn't trigger it. "If it's a creature" is an intervening-if on the triggering permanent,
 * checked when the trigger fires and again on resolution (CR 603.4).
 */
val SaltRoadAmbushers = card("Salt Road Ambushers") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dog Warrior"
    power = 3
    toughness = 3
    oracleText = "Whenever another permanent you control is turned face up, if it's a creature, " +
        "put two +1/+1 counters on it.\n" +
        "Megamorph {3}{G}{G} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)"

    triggeredAbility {
        trigger = Triggers.you.permanentTurnedFaceUp(GameObjectFilter.Permanent.notSourceItself())
        interveningIf = Conditions.EntityMatches(EffectTarget.TriggeringEntity, GameObjectFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.TriggeringEntity)
        description = "Whenever another permanent you control is turned face up, if it's a creature, " +
            "put two +1/+1 counters on it."
    }

    // Megamorph {3}{G}{G}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{3}{G}{G}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Joseph Meehan"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56c1111a-9680-4d34-8cf7-af16ae508a72.jpg?1783938577"
        ruling(
            "2015-02-25",
            "The face-down permanent must be a creature both before it's turned face up and when " +
                "the triggered ability resolves to have +1/+1 counters placed on it.",
        )
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
