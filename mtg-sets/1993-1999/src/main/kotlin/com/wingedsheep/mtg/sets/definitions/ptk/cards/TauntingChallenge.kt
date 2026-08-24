package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MustBeBlockedEffect

/**
 * Taunting Challenge
 * {1}{G}{G}
 * Sorcery
 * All creatures able to block target creature this turn do so.
 */
val TauntingChallenge = card("Taunting Challenge") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "All creatures able to block target creature this turn do so."

    spell {
        val t = target("target", Targets.Creature)
        effect = MustBeBlockedEffect(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Gao Yan"
        flavorText = "Incensed by their opponents' unrelenting taunts, even wise generals were known to rashly lead their troops into battle—often to disastrous defeats."
        imageUri = "https://cards.scryfall.io/normal/front/4/6/4620867c-a3a6-4c81-b923-24007367132e.jpg"
    }
}
