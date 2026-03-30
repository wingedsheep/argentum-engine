package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Might of the Meek
 * {R}
 * Instant
 *
 * Target creature gains trample until end of turn. It also gets +1/+0 until
 * end of turn if you control a Mouse.
 * Draw a card.
 */
val MightOfTheMeek = card("Might of the Meek") {
    manaCost = "{R}"
    typeLine = "Instant"
    oracleText = "Target creature gains trample until end of turn. It also gets +1/+0 until end of turn if you control a Mouse.\nDraw a card."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, creature)
            .then(
                ConditionalEffect(
                    condition = Conditions.ControlCreatureOfType(Subtype("Mouse")),
                    effect = Effects.ModifyStats(1, 0, creature)
                )
            )
            .then(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Danny Schwartz"
        flavorText = "\"Fear is unavoidable. You can run from it with your tail between your legs, or charge it head on with sword in hand. I prefer the latter.\"\n—Rho, veteran hero"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/509bf254-8a2b-4dfa-9ae5-386321b35e8b.jpg?1721426663"
    }
}
