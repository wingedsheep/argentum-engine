package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Defend the Celestus
 * {2}{G}{G}
 * Instant
 * Distribute three +1/+1 counters among one, two, or three target creatures you control.
 */
val DefendTheCelestus = card("Defend the Celestus") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Distribute three +1/+1 counters among one, two, or three target creatures you control."

    spell {
        targets(TargetFilter.CreatureYouControl, count = 3, minCount = 1)
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "182"
        artist = "Andrey Kuzinskiy"
        flavorText = "Witch, werewolf, and cathar stood shoulder to shoulder around the Sungold Lock at the " +
            "center of the ritual, ready to stop Tovolar's assault—or die trying."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c11e4420-06f3-48e2-91d1-a9199483add2.jpg"
    }
}
