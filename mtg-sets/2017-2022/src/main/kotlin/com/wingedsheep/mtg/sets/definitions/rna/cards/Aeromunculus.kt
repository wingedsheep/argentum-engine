package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Aeromunculus — Ravnica Allegiance #152
 * {1}{G}{U} · Creature — Homunculus Mutant · 2 / 3
 *
 * Flying, plus an `Effects.Adapt` activated ability.
 */
val Aeromunculus = card("Aeromunculus") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Homunculus Mutant"
    power = 2
    toughness = 3
    oracleText = "Flying\n{2}{G}{U}: Adapt 1. (If this creature has no +1/+1 counters on it, put a +1/+1 counter on it.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{2}{G}{U}")
        effect = Effects.Adapt(1)
        description = "{2}{G}{U}: Adapt 1."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Simon Dominic"
        flavorText = "\"The absence of binocular vision imposes certain challenges for a flying creature, which are overcome via echolocation.\"\n—Simic research notes"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/671c94c1-df52-4985-ad78-3e1c9c78df18.jpg?1783933659"
        ruling("2019-01-25", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
        ruling("2019-01-25", "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.")
    }
}
