package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Merfolk Falconer — Zendikar Rising #69
 * {3}{U}{U} · Creature — Merfolk Wizard · 4/4
 *
 * Flying
 * Whenever you cast a kicked spell, scry 2.
 */
val MerfolkFalconer = card("Merfolk Falconer") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Wizard"
    oracleText = "Flying\nWhenever you cast a kicked spell, scry 2."
    power = 4
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.WasKicked))
        effect = Effects.Scry(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Brian Valeza"
        flavorText = "\"For generations, we have bred and trained falcons for expeditions into Emeria. Now more " +
            "sky ruins open themselves to those with the means to reach the heights!\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acb0d55a-9d6a-443d-aa82-5cea93188fd2.jpg?1783929392"
    }
}
