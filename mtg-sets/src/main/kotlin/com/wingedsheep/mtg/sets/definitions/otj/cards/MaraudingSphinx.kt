package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Marauding Sphinx
 * {3}{U}{U}
 * Creature — Sphinx Rogue
 * 3/5
 *
 * Flying, vigilance, ward {2}
 * Whenever you commit a crime, surveil 2. This ability triggers only once each turn.
 */
val MaraudingSphinx = card("Marauding Sphinx") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx Rogue"
    power = 3
    toughness = 5
    oracleText = "Flying, vigilance, ward {2}\n" +
        "Whenever you commit a crime, surveil 2. This ability triggers only once each turn. " +
        "(Targeting opponents, anything they control, and/or cards in their graveyards is a crime.)"

    keywords(Keyword.FLYING, Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.ward("{2}"))

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        effect = Patterns.Library.surveil(2)
        description = "Whenever you commit a crime, surveil 2. This ability triggers only once each turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "56"
        artist = "Mila Pesic"
        flavorText = "She poses a riddle to each mark, allowing them a chance to keep their goods. " +
            "Thus far, none have succeeded."
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34071884-c5b6-42c0-9eb3-9f32910c29d8.jpg?1712355453"
    }
}
