package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Storyteller Pixie
 * {3}{U}
 * Creature — Faerie Wizard
 * 3/3
 * Flying
 * Whenever you cast an Adventure spell, draw a card.
 *
 * "An Adventure spell" is the cast-time fact [SpellCastPredicate.CastAsAdventure], not a
 * characteristic of the card — casting the same adventurer as its creature half doesn't trigger
 * this (compare Chancellor of Tales, which reads the same predicate). The trigger fires on cast,
 * so it resolves before the Adventure itself does, and it still draws even if that Adventure is
 * later countered.
 */
val StorytellerPixie = card("Storyteller Pixie") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie Wizard"
    oracleText = "Flying\n" +
        "Whenever you cast an Adventure spell, draw a card."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.youCastSpell(requires = setOf(SpellCastPredicate.CastAsAdventure))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "313"
        artist = "Peter Polach"
        flavorText = "After the royal nanny had finished his dull, droning recital of the prince " +
            "and princess's bedtime story, their secret friend emerged and the real story began."
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9f4cf85-2120-4897-aecd-4ee4b02d6f9b.jpg?1783915040"
    }
}
