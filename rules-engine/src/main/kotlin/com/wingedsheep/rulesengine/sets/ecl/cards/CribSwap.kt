package com.wingedsheep.rulesengine.sets.ecl.cards

import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ExileAndReplaceWithTokenEffect
import com.wingedsheep.rulesengine.ability.cardScript
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.card.Rarity
import com.wingedsheep.rulesengine.card.ScryfallMetadata
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype

/**
 * Crib Swap
 *
 * {2}{W} Kindred Instant — Shapeshifter
 * Changeling (This card is every creature type.)
 * Exile target creature. Its controller creates a 1/1 colorless
 * Shapeshifter creature token with changeling.
 */
object CribSwap {
    val definition = CardDefinition.kindredInstant(
        name = "Crib Swap",
        manaCost = ManaCost.parse("{2}{W}"),
        subtypes = setOf(Subtype.SHAPESHIFTER),
        oracleText = "Changeling (This card is every creature type.)\n" +
                "Exile target creature. Its controller creates a 1/1 colorless " +
                "Shapeshifter creature token with changeling.",
        keywords = setOf(Keyword.CHANGELING),
        metadata = ScryfallMetadata(
            collectorNumber = "11",
            rarity = Rarity.UNCOMMON,
            artist = "Pete Venters",
            flavorText = "Elsewhere in Lorwyn, nestled in a faerie ring, the coos of a small giant turned to cries.",
            imageUri = "https://cards.scryfall.io/normal/front/8/f/8f2fb3c6-af75-47a3-9f97-521872c32890.jpg",
            releaseDate = "2026-01-23"
        )
    )

    val script = cardScript("Crib Swap") {
        keywords(Keyword.CHANGELING)

        // Exile target creature, its controller creates a 1/1 colorless Shapeshifter with changeling
        spell(
            ExileAndReplaceWithTokenEffect(
                target = EffectTarget.TargetCreature,
                tokenPower = 1,
                tokenToughness = 1,
                tokenColors = emptySet(),  // Colorless
                tokenTypes = setOf("Shapeshifter"),
                tokenKeywords = setOf(Keyword.CHANGELING)
            )
        )
    }
}
