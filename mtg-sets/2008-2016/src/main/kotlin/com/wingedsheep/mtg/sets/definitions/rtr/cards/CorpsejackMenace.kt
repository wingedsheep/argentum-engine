package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Corpsejack Menace
 * {2}{B}{G}
 * Creature — Fungus
 * 4/4
 *
 * If one or more +1/+1 counters would be put on a creature you control, twice that many +1/+1
 * counters are put on it instead.
 *
 * Doubling Season's counter half narrowed to +1/+1 counters on creatures you control. It gates
 * only on the recipient ("would be put on"), not the placer, so `placedByYou = false`.
 */
val CorpsejackMenace = card("Corpsejack Menace") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Fungus"
    power = 4
    toughness = 4
    oracleText = "If one or more +1/+1 counters would be put on a creature you control, twice that " +
        "many +1/+1 counters are put on it instead."

    replacementEffect(
        DoubleCounterPlacement(
            placedByYou = false,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterType.PLUS_ONE_PLUS_ONE,
                recipient = Recipient.CreatureYouControl,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Chris Rahn"
        flavorText = "Weakness is not in the nature of the Swarm."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b35a8efe-2a3e-4060-9134-d4150e4bdf28.jpg?1783940342"
        ruling(
            "2017-11-17",
            "If a creature you control would enter the battlefield with a number of +1/+1 counters on it, " +
                "it enters with twice that many instead."
        )
        ruling(
            "2017-11-17",
            "If you control two Corpsejack Menaces, the number of +1/+1 counters put on a creature is four " +
                "times the original number. Three Corpsejack Menaces multiplies the original number by " +
                "eight, and so on."
        )
    }
}
