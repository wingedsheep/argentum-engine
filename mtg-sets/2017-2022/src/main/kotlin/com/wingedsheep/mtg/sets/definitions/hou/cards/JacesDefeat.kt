package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Jace's Defeat
 * {1}{U}
 * Instant
 * Counter target blue spell. If it was a Jace planeswalker spell, scry 2.
 *
 * "Was" is read before the counter: the gate checks the target while it is still on the stack,
 * and both branches counter it, so the scry doesn't depend on the counter succeeding (a Jace
 * spell that can't be countered still lets you scry 2).
 */
val JacesDefeat = card("Jace's Defeat") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target blue spell. If it was a Jace planeswalker spell, scry 2."

    spell {
        val spell = target(TargetFilter.SpellOnStack.withColor(Color.BLUE))
        effect = Effects.If(
            condition = Conditions.EntityMatches(
                spell,
                GameObjectFilter.Planeswalker.withSubtype(Subtype.JACE)
            ),
            then = Effects.CounterSpell() then Effects.Scry(2),
            otherwise = Effects.CounterSpell()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Kieran Yanner"
        flavorText = "\"I had hoped for more from you, Jace. But I expected as much.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6b103c1-9b25-4bfe-9081-570977e9fdad.jpg?1783936052"
    }
}
