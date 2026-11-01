package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Skitter Eel — Ravnica Allegiance #53
 * {3}{U} · Creature — Fish Crab · 3 / 3
 *
 * A vanilla body with an `Effects.Adapt` activated ability.
 */
val SkitterEel = card("Skitter Eel") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Fish Crab"
    power = 3
    toughness = 3
    oracleText = "{2}{U}: Adapt 2. (If this creature has no +1/+1 counters on it, put two +1/+1 counters on it.)"

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        effect = Effects.Adapt(2)
        description = "{2}{U}: Adapt 2."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "53"
        artist = "Mathias Kollros"
        flavorText = "\"Life has no mistakes, only experiments.\"\n—Yolov, Simic bioengineer"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/db328f03-7dae-445b-8e71-99dd88f26a9e.jpg?1783933702"
        ruling("2019-01-25", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
        ruling("2019-01-25", "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.")
    }
}
