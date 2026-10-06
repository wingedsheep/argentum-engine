package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Might of the Masses
 * {G}
 * Instant
 * Target creature gets +1/+1 until end of turn for each creature you control.
 *
 * The count is taken once, on resolution — the bonus is locked in and doesn't change if the number
 * of creatures you control changes later in the turn.
 */
val MightOfTheMasses = card("Might of the Masses") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+1 until end of turn for each creature you control."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(DynamicAmounts.creaturesYouControl(), DynamicAmounts.creaturesYouControl(), t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Johann Bodin"
        flavorText = "The Joraga elves never need ask a troll to leave their territory. They merely grant it " +
            "their combined strength, and it can't resist embarking on a merry rampage."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10465f4f-f4ff-45d8-bc97-3ec85e5eea70.jpg?1783941962"
    }
}
