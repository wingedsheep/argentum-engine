package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanAttackDespiteDefender

/**
 * Scuttlegator — Ravnica Allegiance #218
 * {4}{G/U}{G/U} · Creature — Crab Turtle Crocodile · 6 / 6
 *
 * Defender, an `Effects.Adapt(3)` activated ability, and a `CanAttackDespiteDefender` static gated on
 * a +1/+1 counter on itself.
 */
val Scuttlegator = card("Scuttlegator") {
    manaCost = "{4}{G/U}{G/U}"
    colorIdentity = "GU"
    typeLine = "Creature — Crab Turtle Crocodile"
    power = 6
    toughness = 6
    oracleText = "Defender\n" +
        "{6}{G/U}{G/U}: Adapt 3. (If this creature has no +1/+1 counters on it, put three +1/+1 counters on it.)\n" +
        "As long as this creature has a +1/+1 counter on it, it can attack as though it didn't have defender."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{6}{G/U}{G/U}")
        effect = Effects.Adapt(3)
        description = "{6}{G/U}{G/U}: Adapt 3."
    }

    staticAbility {
        ability = CanAttackDespiteDefender(
            condition = Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "218"
        artist = "Jehan Choo"
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d886fd9a-7800-41f1-b692-32a439b045da.jpg?1783933631"
        ruling("2019-01-25", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
        ruling("2019-01-25", "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.")
    }
}
