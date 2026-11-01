package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeywordByCounter

/**
 * Skatewing Spy — Ravnica Allegiance #52
 * {3}{U} · Creature — Vedalken Rogue Mutant · 2 / 3
 *
 * `Effects.Adapt` activated ability plus Mer-Ek Nightblade's `GrantKeywordByCounter` static.
 */
val SkatewingSpy = card("Skatewing Spy") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Vedalken Rogue Mutant"
    power = 2
    toughness = 3
    oracleText = "{5}{U}: Adapt 2. (If this creature has no +1/+1 counters on it, put two +1/+1 counters on it.)\n" +
        "Each creature you control with a +1/+1 counter on it has flying."

    activatedAbility {
        cost = Costs.Mana("{5}{U}")
        effect = Effects.Adapt(2)
        description = "{5}{U}: Adapt 2."
    }

    // Each creature you control with a +1/+1 counter on it has flying.
    staticAbility { ability = GrantKeywordByCounter(Keyword.FLYING, CounterType.PLUS_ONE_PLUS_ONE, controllerOnly = true) }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "52"
        artist = "Grzegorz Rutkowski"
        flavorText = "\"A better Ravnica begins with a better Simic.\"\n—Vannifar"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef4a517c-515b-4adc-8ea6-fb86820f22ed.jpg?1783933702"
        ruling("2019-01-25", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
        ruling("2019-01-25", "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.")
    }
}
