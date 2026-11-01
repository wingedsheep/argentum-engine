package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Arrester's Admonition
 * {2}{U}
 * Instant
 * Return target creature to its owner's hand.
 * Addendum — If you cast this spell during your main phase, draw a card.
 *
 * Addendum is checked at resolution: [Conditions.WasCast] excludes copies (never cast), and
 * [Conditions.IsYourMainPhase] reads the cast-time phase, since a phase only ends with an empty stack.
 */
val ArrestersAdmonition = card("Arrester's Admonition") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Return target creature to its owner's hand.\n" +
        "Addendum — If you cast this spell during your main phase, draw a card."

    spell {
        val creature = target(TargetFilter.Creature)

        effect = Effects.ReturnToHand(creature) then Effects.If(
            condition = Conditions.All(Conditions.WasCast, Conditions.IsYourMainPhase),
            then = Effects.DrawCards(1),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "31"
        artist = "Lucas Graciano"
        flavorText = "Law is the voice of reason."
        imageUri = "https://cards.scryfall.io/normal/front/6/3/637c2d6a-e6b8-4dc5-81aa-da1b7384e006.jpg?1783933714"
    }
}
