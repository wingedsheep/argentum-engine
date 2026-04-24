package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy

/**
 * Omni-Changeling
 * {3}{U}{U}
 * Creature — Shapeshifter
 * 0/0
 *
 * Changeling (This card is every creature type.)
 * Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell
 * pays for {1} or one mana of that creature's color.)
 * You may have this creature enter as a copy of any creature on the battlefield, except it has changeling.
 */
val OmniChangeling = card("Omni-Changeling") {
    manaCost = "{3}{U}{U}"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "Changeling (This card is every creature type.)\n" +
        "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "You may have this creature enter as a copy of any creature on the battlefield, except it has changeling."

    keywords(Keyword.CHANGELING, Keyword.CONVOKE)

    replacementEffect(
        EntersAsCopy(
            optional = true,
            additionalKeywords = listOf(Keyword.CHANGELING)
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "62"
        artist = "Jeff Laubenstein"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f29ce8f9-42a3-43fa-8197-666cc26e2c76.jpg?1767732596"
        ruling("2025-11-17", "You can choose not to have Omni-Changeling enter as a copy of another creature. If you do, it will just be a 0/0 Shapeshifter with changeling, and unless another effect is increasing its toughness, it will be put into its owner's graveyard.")
        ruling("2025-11-17", "Omni-Changeling copies exactly what was printed on the original creature and nothing else, with the listed exception. It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
        ruling("2025-11-17", "If the copied creature is a token, Omni-Changeling copies the original characteristics of that token as stated by the effect that created that token, with the listed exception.")
        ruling("2025-11-17", "If the copied creature has {X} in its mana cost, X is 0.")
        ruling("2025-11-17", "If the copied creature is copying something else, then Omni-Changeling enters as whatever that creature copied, with the listed exception.")
        ruling("2025-11-17", "Any \"enters\" abilities of the copied creature will trigger when Omni-Changeling enters. Any \"as this creature enters\" or \"this creature enters with\" abilities of the copied creature will also work.")
    }
}
