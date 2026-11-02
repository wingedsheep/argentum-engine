package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeywordByCounter

/**
 * Trollbred Guardian — Ravnica Allegiance #148
 * {4}{G} · Creature — Troll Frog Warrior · 5 / 5
 *
 * An `Effects.Adapt(2)` activated ability and a controller-only `GrantKeywordByCounter` trample
 * static (the same shape as Abzan Battle Priest's lifelink).
 */
val TrollbredGuardian = card("Trollbred Guardian") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Troll Frog Warrior"
    power = 5
    toughness = 5
    oracleText = "{2}{G}: Adapt 2. (If this creature has no +1/+1 counters on it, put two +1/+1 counters on it.)\n" +
        "Each creature you control with a +1/+1 counter on it has trample."

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.Adapt(2)
        description = "{2}{G}: Adapt 2."
    }

    staticAbility {
        ability = GrantKeywordByCounter(Keyword.TRAMPLE, CounterType.PLUS_ONE_PLUS_ONE, controllerOnly = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Mathias Kollros"
        flavorText = "His favorite food is kraul."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e3e51f6-7091-4cf3-86a3-b8c5946f3796.jpg?1783933662"
        ruling("2019-01-25", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
        ruling("2019-01-25", "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.")
    }
}
