package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Kraken of the Straits
 * {5}{U}{U}
 * Creature — Kraken
 * 6/6
 *
 * Creatures with power less than the number of Islands you control can't block this creature.
 *
 * "Power less than N" is "power at most N − 1": the cap is `Islands you control - 1`,
 * left unclamped so that with no Islands a creature with negative power still can't block
 * (its power is less than zero). The block check evaluates it with the Kraken's controller as
 * "you", at the moment blockers are declared.
 *
 * Canonical printing: Born of the Gods, the card's only printing.
 */
val KrakenOfTheStraits = card("Kraken of the Straits") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Kraken"
    power = 6
    toughness = 6
    oracleText = "Creatures with power less than the number of Islands you control can't block this creature."

    staticAbility {
        ability = CantBeBlockedBy(
            GameObjectFilter.Creature.powerAtMostDynamic(
                DynamicAmounts.landsWithSubtype(Subtype.ISLAND) - 1
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Richard Wright"
        flavorText = "Thassa felt no need to punish the sailors for their folly in crossing the straits. The kraken would do it for her."
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12ae69b3-afa5-4763-a0c8-c3d9d96f6ddc.jpg?1783939569"
        ruling("2014-02-01", "The number of Islands you control is counted as blockers are declared. Creatures with power less than that number at that time can't block Kraken of the Straits. Once blockers have been declared, the power of the blocking creatures and the number of Islands you control don't matter.")
    }
}
