package com.wingedsheep.mtg.sets.definitions.jmp.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Branching Evolution
 * {2}{G}
 * Enchantment
 * If one or more +1/+1 counters would be put on a creature you control, twice that many +1/+1
 * counters are put on that creature instead.
 *
 * Doubling Season's counter half narrowed to +1/+1 counters on creatures you control; who places
 * the counters doesn't matter.
 */
val BranchingEvolution = card("Branching Evolution") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "If one or more +1/+1 counters would be put on a creature you control, twice that many +1/+1 counters are put on that creature instead."

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
        collectorNumber = "29"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Is it the water? The stars? Whatever it is, something incredible is happening here.\"\n—Eris, zoologist, journal entry"
        imageUri = "https://cards.scryfall.io/normal/front/4/a/4a6971ad-cbb4-4f66-9bc4-b407c5805e85.jpg?1783930499"
        ruling("2020-06-23", "If a creature you control would enter the battlefield with a number of +1/+1 counters on it, it enters with twice that many instead.")
        ruling("2020-06-23", "If you control two Branching Evolutions, the number of +1/+1 counters put on a creature is four times the original number. Three Branching Evolutions multiplies the original number by eight, and so on.")
        ruling("2020-06-23", "If two or more effects attempt to modify how many counters would be put onto a creature you control, you choose the order to apply those effects, no matter who controls the sources of those effects.")
    }
}
