package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CopyEntryCounters
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyExceptions

/**
 * Spark Double — War of the Spark #68
 * {3}{U} · Creature — Illusion · Rare
 * 0/0
 *
 * You may have this creature enter as a copy of a creature or planeswalker you control, except it
 * enters with an additional +1/+1 counter on it if it's a creature, it enters with an additional
 * loyalty counter on it if it's a planeswalker, and it isn't legendary.
 *
 * The two counter clauses are two `entersWithCounters` riders, each asking about the copy itself
 * (`ifEntrantMatches`), so a creature planeswalker copy gets both. The loyalty counter lands on top
 * of the copied planeswalker's printed starting loyalty. "Isn't legendary" is a copiable
 * `removedSupertypes` exception.
 */
val SparkDouble = card("Spark Double") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Illusion"
    power = 0
    toughness = 0
    oracleText = "You may have this creature enter as a copy of a creature or planeswalker you control, " +
        "except it enters with an additional +1/+1 counter on it if it's a creature, it enters with an " +
        "additional loyalty counter on it if it's a planeswalker, and it isn't legendary."

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.CreatureOrPlaneswalker.youControl(),
            exceptions = CopyExceptions(removedSupertypes = setOf(Supertype.LEGENDARY)),
            entersWithCounters = listOf(
                CopyEntryCounters(
                    count = DynamicAmounts.fixed(1),
                    ifEntrantMatches = GameObjectFilter.Creature,
                ),
                CopyEntryCounters(
                    count = DynamicAmounts.fixed(1),
                    counterType = CounterType.LOYALTY,
                    ifEntrantMatches = GameObjectFilter.Planeswalker,
                ),
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb8a103c-b776-4501-9441-a45b90391045.jpg?1783933455"
        ruling(
            "2019-05-03",
            "Spark Double isn't legendary if it copies a legendary permanent, and this exception is copiable. " +
                "If something else copies Spark Double later, that copy also won't be legendary."
        )
        ruling(
            "2019-05-03",
            "If it copies a planeswalker, Spark Double enters with the appropriate number of loyalty counters as " +
                "printed on the card plus one. If it copies a creature, Spark Double enters with a +1/+1 counter on " +
                "it, plus any counters that will be put on it from abilities it copied and other abilities of other objects."
        )
        ruling(
            "2019-05-03",
            "If Spark Double somehow enters the battlefield at the same time as another permanent, it can't become " +
                "a copy of that permanent. You may choose only a creature or planeswalker that's already on the battlefield."
        )
    }
}
