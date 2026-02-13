package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyAndChainCopyEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

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
        target = TargetPermanent(filter = TargetFilter.NoncreaturePermanent)
        effect = DestroyAndChainCopyEffect(
            target = EffectTarget.ContextTarget(0),
            targetFilter = TargetFilter.NoncreaturePermanent,
            spellName = "Chain of Acid"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "252"
        artist = "Arnie Swekel"
        flavorText = "\"We have no quarrel with you,\" said the elf. \"But neither do we have sympathy.\""
        imageUri = "https://cards.scryfall.io/large/front/1/d/1d0aec63-e44e-4ec3-82e2-e5da44119725.jpg?1562901973"
    }
}
