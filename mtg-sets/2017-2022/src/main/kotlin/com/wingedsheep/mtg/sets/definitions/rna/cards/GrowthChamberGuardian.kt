package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Growth-Chamber Guardian — Ravnica Allegiance #128
 * {1}{G} · Creature — Elf Crab Warrior · 2 / 2
 *
 * The counter trigger is Herd Baloth's per-batch `Triggers.self.getsCounters(PLUS_ONE_PLUS_ONE)`
 * ("one or more … are put on"); the tutor is Daru Cavalier's self-named library search, wrapped in
 * `Effects.May` for "you may".
 */
val GrowthChamberGuardian = card("Growth-Chamber Guardian") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Crab Warrior"
    power = 2
    toughness = 2
    oracleText = "{2}{G}: Adapt 2. (If this creature has no +1/+1 counters on it, put two +1/+1 counters on it.)\n" +
        "Whenever one or more +1/+1 counters are put on this creature, you may search your library for a card " +
        "named Growth-Chamber Guardian, reveal it, put it into your hand, then shuffle."

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.Adapt(2)
        description = "{2}{G}: Adapt 2."
    }

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.Any.named("Growth-Chamber Guardian"),
                count = 1,
                destination = SearchDestination.HAND,
                reveal = true,
                shuffleAfter = true
            )
        )
        description = "Whenever one or more +1/+1 counters are put on this creature, you may search your " +
            "library for a card named Growth-Chamber Guardian, reveal it, put it into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Bram Sels"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f97cdf4-231d-4bd0-af5e-bcb64ce1556c.jpg?1783933671"
        ruling("2019-01-25", "An ability that triggers when counters are put on a permanent will trigger if that permanent somehow enters the battlefield with those counters.")
        ruling("2019-01-25", "You can always activate an ability that will cause a creature to adapt. As that ability resolves, if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.")
        ruling("2019-01-25", "If a creature somehow loses all of its +1/+1 counters, it can adapt again and get more +1/+1 counters.")
    }
}
