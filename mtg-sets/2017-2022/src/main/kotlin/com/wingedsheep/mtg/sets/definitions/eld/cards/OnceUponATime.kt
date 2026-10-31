package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SelfAlternativeCost
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Once Upon a Time
 * {1}{G}
 * Instant
 * If this spell is the first spell you've cast this game, you may cast it without paying its mana cost.
 * Look at the top five cards of your library. You may reveal a creature or land card from among them
 * and put it into your hand. Put the rest on the bottom of your library in a random order.
 *
 * The free cast is a {0} [SelfAlternativeCost] offered while you've cast no spells this game — the
 * gate is judged as the spell is being cast, before it counts itself.
 */
val OnceUponATime = card("Once Upon a Time") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "If this spell is the first spell you've cast this game, you may cast it without paying its " +
        "mana cost.\nLook at the top five cards of your library. You may reveal a creature or land card from " +
        "among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    selfAlternativeCost = SelfAlternativeCost(
        manaCost = ManaCost.ZERO,
        condition = Conditions.CompareAmounts(DynamicAmounts.spellsCastThisGame(), ComparisonOperator.EQ, 0),
    )

    spell {
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 5,
            filter = GameObjectFilter.CreatureOrLand,
            prompt = "You may reveal a creature or land card from among them and put it into your hand",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "169"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/4034e5ba-9974-43e3-bde7-8d9b4586c3a4.jpg?1783932607"
    }
}
