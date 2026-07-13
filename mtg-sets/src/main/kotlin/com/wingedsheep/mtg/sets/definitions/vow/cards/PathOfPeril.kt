package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Path of Peril
 * {1}{B}{B}
 * Sorcery
 * Cleave {4}{W}{B} (You may cast this spell for its cleave cost. If you do, remove the words in
 * square brackets.)
 * Destroy all creatures [with mana value 2 or less].
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * cast is a narrow sweeper (only creatures with mana value 2 or less); the cleaved cast is an
 * unconditional wrath.
 *
 * A mass-destroy has no target, so only the effect differs between the two modes — the base
 * [effect] filters by mana value and the [cleaveEffect] hits every creature. The engine picks the
 * cleave effect when the spell is cast for its cleave cost.
 */
val PathOfPeril = card("Path of Peril") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "BW"
    typeLine = "Sorcery"
    oracleText = "Cleave {4}{W}{B} (You may cast this spell for its cleave cost. If you do, " +
        "remove the words in square brackets.)\nDestroy all creatures [with mana value 2 or less]."

    keywordAbility(KeywordAbility.cleave("{4}{W}{B}"))

    spell {
        // Printed (brackets present): destroy all creatures with mana value 2 or less.
        effect = Effects.DestroyAll(filter = GameObjectFilter.Creature.manaValueAtMost(2))

        // Cleaved (brackets removed): destroy all creatures.
        cleaveEffect = Effects.DestroyAll(filter = GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Kasia 'Kafis' Zielińska"
        flavorText = "To Olivia's guests, a welcome. To everyone else, a warning."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0c5449a-d63b-4b22-9432-8f0365c3c4d9.jpg?1782703101"
    }
}
