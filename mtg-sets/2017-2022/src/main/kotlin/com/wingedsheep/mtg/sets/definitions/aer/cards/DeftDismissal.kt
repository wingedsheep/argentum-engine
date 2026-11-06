package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Deft Dismissal
 * {3}{W}
 * Instant
 * Deft Dismissal deals 3 damage divided as you choose among one, two, or three target attacking or
 * blocking creatures.
 */
val DeftDismissal = card("Deft Dismissal") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Deft Dismissal deals 3 damage divided as you choose among one, two, or three target attacking or blocking creatures."

    spell {
        target = TargetObject(filter = TargetFilter.AttackingOrBlockingCreature, count = 3, minCount = 1)
        effect = Effects.DividedDamage(total = 3, minTargets = 1, maxTargets = 3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "17"
        artist = "Izzy"
        flavorText = "\"With spy thopters all around, how can the consuls be so blind to Tezzeret's manipulations?\"\n—Gideon Jura"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cb26d5b-ffeb-4dac-ad61-f85e5e7b1675.jpg?1783936780"
        ruling("2017-02-09", "You choose how many targets Deft Dismissal has and how the damage is divided as you cast the spell. Each target must receive at least 1 damage.")
        ruling("2017-02-09", "If some (but not all) of the targets become illegal, the original division of damage still applies, but no damage is dealt to illegal targets. If all targets become illegal, Deft Dismissal doesn't resolve.")
    }
}
