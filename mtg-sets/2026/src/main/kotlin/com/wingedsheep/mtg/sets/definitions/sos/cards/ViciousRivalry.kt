package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Vicious Rivalry
 * {2}{B}{G}
 * Sorcery
 * As an additional cost to cast this spell, pay X life.
 * Destroy all artifacts and creatures with mana value X or less.
 *
 * X is declared at cast time via the [Costs.additional.PayXLife] additional cost (capped at the
 * caster's life total). That single X is surfaced to the spell's resolution as the X value, so the
 * board wipe filters on `manaValueAtMostX()` ("mana value X or less"). The card has no
 * `{X}` in its mana cost, so there's no collision over the shared X slot.
 */
val ViciousRivalry = card("Vicious Rivalry") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, pay X life.\n" +
        "Destroy all artifacts and creatures with mana value X or less."

    additionalCost(Costs.additional.PayXLife())

    spell {
        effect = Effects.DestroyAll(
            filter = GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(listOf(CardPredicate.IsArtifact, CardPredicate.IsCreature)),
                    CardPredicate.CompareNumericProperty(CardNumericProperty.MANA_VALUE, ComparisonOperator.LTE, DynamicAmounts.xValue()),
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "241"
        artist = "Chris Rallis"
        flavorText = "So intense was their hatred they didn't realize how lonely they'd become. " +
            "In the end, all they had was each other, and they hated that, too."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6fa9cd18-3181-4373-ab65-49bf9de9487f.jpg?1775938681"
    }
}
