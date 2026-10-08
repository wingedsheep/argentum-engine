package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Fix What's Broken
 * {2}{W}{B}
 * Sorcery
 *
 * As an additional cost to cast this spell, pay X life.
 * Return each artifact and creature card with mana value X from your graveyard to the battlefield.
 *
 * X is declared at cast time via the [Costs.additional.PayXLife] additional cost (capped at the
 * caster's life total), and surfaced to resolution as the spell's X value. At resolution the
 * gather→move pipeline collects every card in the caster's graveyard that is an artifact or a
 * creature *and* has mana value exactly X (`manaValueEqualsX()`), then returns them all
 * to the battlefield under their owner's control. The card has no `{X}` in its mana cost, so there
 * is no collision over the shared X slot.
 */
val FixWhatsBroken = card("Fix What's Broken") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, pay X life.\n" +
        "Return each artifact and creature card with mana value X from your graveyard to the battlefield."

    additionalCost(Costs.additional.PayXLife())

    spell {
        effect = Effects.Pipeline {
            val reanimate = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.You,
                    filter = GameObjectFilter(
                        cardPredicates = listOf(
                            CardPredicate.Or(listOf(CardPredicate.IsArtifact, CardPredicate.IsCreature)),
                            CardPredicate.CompareNumericProperty(CardNumericProperty.MANA_VALUE, ComparisonOperator.EQ, DynamicAmounts.xValue()),
                        ),
                    ),
                )
            )
            move(reanimate, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "188"
        artist = "Chris Rallis"
        flavorText = "Two tired souls finally found something to unite them."
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c0cd1d71-8e4a-4e00-80cd-83aec231fa57.jpg?1775938304"
    }
}
