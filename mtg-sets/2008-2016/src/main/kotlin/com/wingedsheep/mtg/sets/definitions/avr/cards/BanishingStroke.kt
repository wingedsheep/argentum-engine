package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Banishing Stroke
 * {5}{W}
 * Instant
 *
 * Put target artifact, creature, or enchantment on the bottom of its owner's library.
 * Miracle {W} (You may cast this card for its miracle cost when you draw it if it's the first card you drew this turn.)
 *
 * Same shape as Vanishment, moving to the bottom instead of the top of the library.
 */
val BanishingStroke = card("Banishing Stroke") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Put target artifact, creature, or enchantment on the bottom of its owner's library.\n" +
        "Miracle {W} (You may cast this card for its miracle cost when you draw it if it's the first " +
        "card you drew this turn.)"

    spell {
        val permanent = target(TargetFilter.ArtifactCreatureOrEnchantment)
        effect = Effects.PutOnBottomOfLibrary(permanent)
    }

    keywordAbility(KeywordAbility.miracle("{W}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "7"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/238d8437-1abd-4bb7-8b5b-54f959bc2c79.jpg?1783940740"
    }
}
