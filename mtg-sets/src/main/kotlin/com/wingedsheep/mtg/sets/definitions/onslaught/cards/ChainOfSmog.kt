package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DiscardAndChainCopyEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Chain of Smog
 * {1}{B}
 * Sorcery
 * Target player discards two cards. That player may copy this spell and may choose
 * a new target for that copy.
 */
val ChainOfSmog = card("Chain of Smog") {
    manaCost = "{1}{B}"
    typeLine = "Sorcery"
    oracleText = "Target player discards two cards. That player may copy this spell and may choose a new target for that copy."

    spell {
        target = TargetPlayer()
        effect = DiscardAndChainCopyEffect(
            count = 2,
            target = EffectTarget.ContextTarget(0),
            spellName = "Chain of Smog"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Greg Staples"
        imageUri = "https://cards.scryfall.io/large/front/6/b/6bfe64f9-8b03-41f6-a47b-fade397ad9d1.jpg?1562920423"
    }
}
