package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Chain of Acid
 * {3}{G}
 * Sorcery
 * Destroy target noncreature permanent. Then that permanent's controller may copy this spell
 * and may choose a new target for that copy.
 */
val ChainOfAcid = card("Chain of Acid") {
    manaCost = "{3}{G}"
    typeLine = "Sorcery"
    oracleText = "Destroy target noncreature permanent. Then that permanent's controller may copy this spell and may choose a new target for that copy."

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter.NoncreaturePermanent))
        effect = Effects.DestroyAndChainCopy(
            target = t,
            targetFilter = TargetFilter.NoncreaturePermanent,
            spellName = "Chain of Acid"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "252"
        artist = "Arnie Swekel"
        flavorText = "\"We have no quarrel with you,\" said the elf. \"But neither do we have sympathy.\""
        imageUri = "https://cards.scryfall.io/large/front/1/d/1d47ddca-a363-4ab7-b7f2-d0e0043c9916.jpg?1562901859"
    }
}
