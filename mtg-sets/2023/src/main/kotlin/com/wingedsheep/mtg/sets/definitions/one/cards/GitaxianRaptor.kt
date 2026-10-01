package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gitaxian Raptor
 * {2}{U}
 * Creature — Phyrexian Bird
 * 1/4
 *
 * Flying
 * This creature enters with three oil counters on it.
 * Remove an oil counter from this creature: This creature gets +1/-1 until end of turn.
 */
val GitaxianRaptor = card("Gitaxian Raptor") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Phyrexian Bird"
    power = 1
    toughness = 4
    oracleText = "Flying\n" +
        "This creature enters with three oil counters on it.\n" +
        "Remove an oil counter from this creature: This creature gets +1/-1 until end of turn."

    keywords(Keyword.FLYING)

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 3, selfOnly = true))

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.OIL, 1)
        effect = Effects.ModifyStats(1, -1, EffectTarget.Self)
        description = "Remove an oil counter from this creature: This creature gets +1/-1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Maxime Minard"
        flavorText = "Bored with the ruins of Lumengrid, it migrated to the ruins of the Mephidross."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f5e95f8-c04d-405f-bba4-e83a8f6bf463.jpg?1783918064"
    }
}
