package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.evolve
import com.wingedsheep.sdk.model.Rarity

/**
 * Scurry Oak — Modern Horizons 2 #172
 * {2}{G} · Creature — Treefolk · 1 / 2
 *
 * Evolve
 * Whenever one or more +1/+1 counters are put on this creature, you may create a 1/1 green
 * Squirrel creature token.
 *
 * The [evolve] keyword trigger plus Herd Baloth's per-placement `Triggers.self.getsCounters` watcher
 * with `optional = true` for the "you may".
 */
val ScurryOak = card("Scurry Oak") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Treefolk"
    power = 1
    toughness = 2
    oracleText = "Evolve (Whenever a creature you control enters, if that creature has greater power or " +
        "toughness than this creature, put a +1/+1 counter on this creature.)\n" +
        "Whenever one or more +1/+1 counters are put on this creature, you may create a 1/1 green Squirrel creature token."

    evolve()

    triggeredAbility {
        trigger = Triggers.self.getsCounters(CounterType.PLUS_ONE_PLUS_ONE)
        optional = true
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Squirrel"),
            imageUri = "https://cards.scryfall.io/normal/front/9/7/977ddd05-1aae-46fc-95ce-866710d1c5c6.jpg?1783926589"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "172"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fb48c2e-ee0f-4fae-9c22-247870c10d5b.jpg?1783926825"
        ruling("2021-06-18", "When comparing the characteristics of the two creatures for evolve, you always compare power to power and toughness to toughness.")
        ruling("2021-06-18", "If evolve triggers, the comparison will happen again when the ability tries to resolve. If neither characteristic of the new creature is greater, the ability will do nothing. If the creature that entered the battlefield leaves the battlefield before evolve resolves, use its last known power and toughness to determine whether the creature with evolve gets a +1/+1 counter.")
    }
}
