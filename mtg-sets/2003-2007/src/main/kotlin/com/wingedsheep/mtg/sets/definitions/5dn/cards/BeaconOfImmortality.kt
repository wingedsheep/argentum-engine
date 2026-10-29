package com.wingedsheep.mtg.sets.definitions.`5dn`.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity

/**
 * Beacon of Immortality
 * {5}{W}
 * Instant
 * Double target player's life total. Shuffle Beacon of Immortality into its owner's library.
 *
 * Doubling a life total (CR 701.10d) makes the player gain or lose the life needed to reach twice
 * the old total, so it is `SetLifeTotal(lifeTotal × 2)`: `SetLifeTotalExecutor` turns an increase
 * into a real life gain (life-gain replacements and triggers apply, per the 2007-07-15 ruling) and
 * a negative total into the matching loss (-10 becomes -20). `selfShuffleIntoLibrary()` replaces
 * the CR 608.2n graveyard destination; a countered Beacon still goes to the graveyard.
 */
val BeaconOfImmortality = card("Beacon of Immortality") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Double target player's life total. Shuffle Beacon of Immortality into its owner's library."

    spell {
        val player = target(Targets.Player)
        effect = Effects.SetLifeTotal(DynamicAmounts.lifeTotal(player.asPlayer) * 2, player)
        selfShuffleIntoLibrary()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Rob Alexander"
        flavorText = "The cave floods with light. A thousand rays shine forth and meld into one."
        imageUri = "https://cards.scryfall.io/normal/front/6/0/601bb6d1-77f4-4ffe-94d0-bed74c33013b.jpg?1783944411"
        ruling("2007-07-15", "Beacon of Immortality's effect counts as life gain (or life loss, if the life total was negative) for effects that trigger on or replace life gain (or life loss).")
        ruling("2007-07-15", "If a Beacon is countered or doesn't resolve, it's put into its owner's graveyard, not shuffled into the library.")
        ruling("2007-02-01", "If you double a negative life total, you do the real math. A life total of -10 becomes -20.")
    }
}
