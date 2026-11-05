package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Compelling Argument
 * {1}{U}
 * Sorcery
 * Target player mills five cards.
 * Cycling {U} ({U}, Discard this card: Draw a card.)
 */
val CompellingArgument = card("Compelling Argument") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target player mills five cards.\n" +
        "Cycling {U} ({U}, Discard this card: Draw a card.)"

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(5, player)
    }

    keywordAbility(KeywordAbility.cycling("{U}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "47"
        artist = "Lucas Graciano"
        flavorText = "\"How can there be doubt in the presence of such blessings?\"\n—Ukhat, vizier of initiation"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c35f4d5-2337-4ea0-a571-5a73e2d0d54a.jpg"
    }
}
