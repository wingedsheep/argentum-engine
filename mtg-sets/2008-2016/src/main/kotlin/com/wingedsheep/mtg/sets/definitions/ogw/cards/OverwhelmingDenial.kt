package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Overwhelming Denial
 * {2}{U}{U}
 * Instant
 * Surge {U}{U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)
 * This spell can't be countered.
 * Counter target spell.
 */
val OverwhelmingDenial = card("Overwhelming Denial") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Surge {U}{U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)\n" +
        "This spell can't be countered.\n" +
        "Counter target spell."

    cantBeCountered = true
    keywordAbility(KeywordAbility.surge("{U}{U}"))

    spell {
        target(TargetFilter.SpellOnStack)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Jama Jurabaev"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33ff1000-1a4e-43f6-aa02-1dbe9fac6901.jpg?1783937918"
    }
}
