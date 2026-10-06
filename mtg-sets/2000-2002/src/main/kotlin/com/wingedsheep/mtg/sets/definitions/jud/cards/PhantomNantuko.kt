package com.wingedsheep.mtg.sets.definitions.jud.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.PreventDamageByRemovingCounter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phantom Nantuko
 * {2}{G}
 * Creature — Insect Spirit
 * 0/0
 * Trample
 * This creature enters with two +1/+1 counters on it.
 * If damage would be dealt to this creature, prevent that damage. Remove a +1/+1 counter from this creature.
 * {T}: Put a +1/+1 counter on this creature.
 *
 * The damage clause is [PreventDamageByRemovingCounter] at its defaults: one counter per damage
 * event however many sources, prevention even with no counter left, and — when the damage can't be
 * prevented — the damage is dealt and a counter is still removed. All three are the printed rulings.
 */
val PhantomNantuko = card("Phantom Nantuko") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect Spirit"
    oracleText = "Trample\n" +
        "This creature enters with two +1/+1 counters on it.\n" +
        "If damage would be dealt to this creature, prevent that damage. Remove a +1/+1 counter from this creature.\n" +
        "{T}: Put a +1/+1 counter on this creature."
    power = 0
    toughness = 0

    keywords(Keyword.TRAMPLE)

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 2,
        selfOnly = true
    ))

    replacementEffect(PreventDamageByRemovingCounter())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Wayne England"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66f8ca45-b60f-4bb9-9f7e-1b5e13478f22.jpg?1783945109"
        ruling("2022-12-08", "If this creature would be dealt damage from multiple sources at the same time (say, because it's blocked by multiple creatures), all of the damage is prevented and only one +1/+1 counter is removed.")
        ruling("2022-12-08", "If damage that would be dealt to this creature can't be prevented, the damage is dealt and a +1/+1 counter is removed from this creature.")
        ruling("2022-12-08", "If this creature has no +1/+1 counters on it (but is still on the battlefield because something else is raising its toughness), damage that would be dealt to it is still prevented.")
    }
}
