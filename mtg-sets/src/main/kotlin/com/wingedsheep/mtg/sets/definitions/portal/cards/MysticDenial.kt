package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CounterSpellWithFilterEffect
import com.wingedsheep.sdk.scripting.SpellFilter

/**
 * Mystic Denial
 * {1}{U}{U}
 * Instant
 * Counter target creature or sorcery spell.
 */
val MysticDenial = card("Mystic Denial") {
    manaCost = "{1}{U}{U}"
    typeLine = "Instant"

    spell {
        effect = CounterSpellWithFilterEffect(SpellFilter.CreatureOrSorcery())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Hannibal King"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c93d4e9-7fd6-4814-b86b-89b92d1dad3b.jpg"
    }
}
